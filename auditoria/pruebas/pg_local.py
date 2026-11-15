"""Postgres LOCAL y desechable para probar las migraciones (no es Supabase).

Crea un clúster temporal con initdb (como usuario `postgres` si corremos como root), con
stubs mínimos de Supabase: roles anon/authenticated/service_role, auth.users, auth.uid()/auth.jwt()
leyendo `request.jwt.claims`, y storage.buckets/objects. Habla con el servidor solo con `psql`.
"""
import atexit
import os
import shutil
import subprocess
import tempfile

BIN = os.environ.get("PG_BIN", "/usr/lib/postgresql/16/bin")

STUBS = r"""
do $$ begin
  create role anon nologin; exception when duplicate_object then null; end $$;
do $$ begin
  create role authenticated nologin; exception when duplicate_object then null; end $$;
do $$ begin
  create role service_role nologin bypassrls; exception when duplicate_object then null; end $$;
create schema if not exists auth;
create table if not exists auth.users (id uuid primary key, email text);
create or replace function auth.jwt() returns jsonb language sql stable as
$$ select coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb $$;
create or replace function auth.uid() returns uuid language sql stable as
$$ select nullif(auth.jwt()->>'sub', '')::uuid $$;
create or replace function auth.role() returns text language sql stable as
$$ select auth.jwt()->>'role' $$;
grant usage on schema auth to anon, authenticated, service_role;
grant execute on all functions in schema auth to anon, authenticated, service_role;
create schema if not exists storage;
create table if not exists storage.buckets (id text primary key, name text, public boolean, file_size_limit bigint, allowed_mime_types text[]);
create table if not exists storage.objects (id uuid default gen_random_uuid() primary key, bucket_id text, name text,
  metadata jsonb, created_at timestamptz default now(), owner uuid);
alter table storage.objects enable row level security;
grant usage on schema public, storage to anon, authenticated, service_role;
alter default privileges in schema public grant all on tables to anon, authenticated, service_role;
alter default privileges in schema public grant all on sequences to anon, authenticated, service_role;
alter default privileges in schema public grant execute on functions to anon, authenticated, service_role;
"""


class ErrorSQL(Exception):
    pass


class Pg:
    def __init__(self):
        self.dir = tempfile.mkdtemp(prefix="checa-pg-")
        self.root = os.geteuid() == 0
        if self.root:
            os.chmod(self.dir, 0o777)
            shutil.chown(self.dir, "postgres")
        data = os.path.join(self.dir, "data")
        self._como(["initdb", "-D", data, "-A", "trust", "-U", "postgres", "--no-sync", "-E", "UTF8", "--locale=C"])
        self._como(["pg_ctl", "-D", data, "-w", "-l", os.path.join(self.dir, "log"), "-o",
                    f"-k {self.dir} -c listen_addresses='' -c fsync=off", "start"])
        self.data = data
        atexit.register(self.cerrar)
        self.sql("create database prueba")
        self.db = "prueba"
        self.sql(STUBS)

    def _como(self, cmd):
        cmd = [os.path.join(BIN, cmd[0])] + cmd[1:]
        if self.root:
            cmd = ["runuser", "-u", "postgres", "--"] + cmd
        subprocess.run(cmd, check=True, capture_output=True)

    def version(self):
        return self.sql("show server_version").strip()

    def sql(self, texto, rol=None, claims=None, db=None):
        """Ejecuta SQL; devuelve la salida (-At, separador |). Con `rol` simula una petición de PostgREST."""
        pre = ""
        if rol:
            import json
            pre = f"set role {rol};\nselect set_config('request.jwt.claims', '{json.dumps(claims or {'role': rol})}', false);\n"
        p = subprocess.run([os.path.join(BIN, "psql"), "-X", "-q", "-At", "-v", "ON_ERROR_STOP=1", "-h", self.dir, "-U", "postgres",
                            "-d", db or getattr(self, "db", "postgres"), "-F", "|"],
                           input=pre + texto, capture_output=True, text=True)
        if p.returncode:
            raise ErrorSQL(p.stderr.strip())
        out = p.stdout
        if rol:  # quita la línea del set_config
            out = "\n".join(out.splitlines()[1:])
        return out

    def archivo(self, ruta, **k):
        with open(ruta, encoding="utf-8") as f:
            return self.sql(f.read(), **k)

    def cerrar(self):
        if getattr(self, "data", None):
            try:
                self._como(["pg_ctl", "-D", self.data, "-m", "immediate", "stop"])
            except Exception:
                pass
            self.data = None
            shutil.rmtree(self.dir, ignore_errors=True)

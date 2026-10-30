# Skills que se borraron (16 oct 2026)

Se quitaron del repo para que no se carguen en cada conversación. Si algún día hace falta una, se reinstala de su fuente (o se recupera del historial de git, carpeta `.claude/skills-archivo/`).

- **coreyhaines31/marketingskills**: ad-creative, ads, ai-seo, analytics, aso, attribution, churn-prevention, co-marketing, cold-email, community-marketing, competitor-profiling, competitors, content-strategy, customer-research, directory-submissions, emails, events, free-tools, image, influencer-marketing, launch, lead-magnets, marketing-council, marketing-ideas, marketing-loops, marketing-plan, offers, onboarding, paywalls, popups, programmatic-seo, prospecting, public-relations, referrals, revops, sales-enablement, schema, seo-audit, signup, site-architecture, sms
- **heygen-com/hyperframes**: embedded-captions, faceless-explainer, figma, general-video, motion-graphics, music-to-video, pr-to-video, remotion-to-hyperframes, slideshow, talking-head-recut

## Segunda limpieza (30 oct 2026, Eduardo)
Se quitaron porque sus descripciones se cargaban en cada conversación (~4 mil tokens) y casi no se usaban; algunas además competían con las skills propias (`guion-checa`, `video-checa`). Se recuperan del historial de git (`git log --diff-filter=D -- .claude/skills/<nombre>`) o se reinstalan de su fuente.
- **heygen-com/hyperframes**: hyperframes, hyperframes-animation, hyperframes-audio, hyperframes-cli, hyperframes-core, hyperframes-creative, hyperframes-keyframes, hyperframes-registry, hyperframes-studio, media-use, product-launch-video. Los videos usan el CLI (`npx hyperframes`) a través de `video/scripts/checa.py`; la regla es no usar la skill `hyperframes` como entrada (costó ~1.7 M de tokens).
- **coreyhaines31/marketingskills**: video, ab-testing, cro, pricing, product-marketing, marketing-psychology, social, copy-editing.
- **ui-ux-pro-max**.
Se quedan: `guion-checa`, `video-checa`, `marca-checa` y `copywriting` (textos de la web).

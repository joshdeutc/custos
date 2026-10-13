# Custos website

One-page showcase site for Custos, plus its privacy policy. Plain static files: no build step,
no framework, no cookies, no trackers, no external requests (system fonts, self-hosted assets).

| Path | Content |
|---|---|
| `index.html` | Landing page, French (default) |
| `en/index.html` | Landing page, English |
| `confidentialite.html` | Privacy policy, French |
| `en/privacy.html` | Privacy policy, English |
| `styles.css` | The only stylesheet (light and dark via `prefers-color-scheme`) |
| `assets/` | Logo (copy of `logo_custos.svg`), Open Graph images, PNG icon |
| `tools/` | Sources of the PNG images and the script that renders them (not needed at runtime) |

## Preview locally

From the repo root, serve the folder with any static server, for example:

```bash
python -m http.server 8080 --directory website
# or
npx serve website
```

Then open http://localhost:8080. Opening `index.html` directly from disk also works.

## Before going live

1. **Privacy policy placeholders** (highlighted in yellow on both privacy pages): publisher name,
   contact email and the hosting provider. The Play Console needs the public URL of
   `confidentialite.html` (or `en/privacy.html`).
2. **Android backup**: the manifest does not set `android:allowBackup`, so Android Auto Backup is
   on by default and the privacy pages say so. If you set `android:allowBackup="false"`, delete
   that paragraph (marked with a `TODO(Joshua)` comment) in both privacy pages.
3. **Domain**: once known, uncomment and fill the `TODO(domain)` block in the `<head>` of each page
   (canonical, hreflang, `og:url`) and make `og:image` / `twitter:image` absolute
   (`https://YOUR-DOMAIN/assets/og-fr.png`). Most social networks ignore relative image URLs.
4. **GitHub release**: every download button points to
   `https://github.com/joshdeutc/custos/releases/latest`. The latest published release is v1.1,
   while the code is at v1.3: publish a newer release with its APK so visitors get it.
   Tip: if each release also ships the APK under a fixed name (for example `custos.apk`), the
   buttons can link to `https://github.com/joshdeutc/custos/releases/latest/download/custos.apk`
   and start the download in one tap.
5. **Google Play**: when the listing is live, replace the two "Bientôt sur Google Play" /
   "Coming soon to Google Play" badges (`<span class="store-soon">`) with a link to
   `https://play.google.com/store/apps/details?id=com.jo.selfcontrol.ultimate`, ideally using the
   official badge from https://play.google.com/intl/en_us/badges/, and update the matching FAQ
   answer and the final call to action.
6. **Screenshots**: the hero shows an illustration (logo in a phone frame), not a screenshot.
   When real screenshots exist (see `assets/screenshots/`), the phone frame can hold one: replace
   the content of `.phone-screen` with an `<img>`.

## Deploy

Any static host works; publish the `website/` folder as the site root.

- **GitHub Pages**: "Deploy from a branch" only serves the repo root or `/docs`, so use a
  workflow instead. Set Settings > Pages > Source to "GitHub Actions" and add
  `.github/workflows/pages.yml`:

  ```yaml
  name: Deploy website
  on:
    push:
      branches: [master]
      paths: ["website/**"]
    workflow_dispatch:
  permissions:
    contents: read
    pages: write
    id-token: write
  concurrency:
    group: pages
    cancel-in-progress: true
  jobs:
    deploy:
      runs-on: ubuntu-latest
      environment:
        name: github-pages
        url: ${{ steps.deployment.outputs.page_url }}
      steps:
        - uses: actions/checkout@v4
        - uses: actions/upload-pages-artifact@v3
          with:
            path: website
        - id: deployment
          uses: actions/deploy-pages@v4
  ```

  The site is then served at `https://joshdeutc.github.io/custos/` (all links are relative, so the
  sub-path works) or at a custom domain.
- **Cloudflare Pages / Netlify**: connect the repo, no build command, output directory `website`.
- **Railway**: create a service from the repo and set its root directory to `/website`; Railway
  serves a folder with an `index.html` as a static site. A custom domain can be added in the
  service settings.

## Regenerate the images

`assets/og-fr.png`, `assets/og-en.png` (1200x630) and `assets/icon-180.png` are rendered from
`tools/og.html` and `tools/icon.html` with a local Chrome (Node 22+, no npm dependency):

```bash
node website/tools/render-images.mjs
```

Set `CHROME_PATH` if Chrome is not found automatically.

# glovebox.cosgriff.io

Static files served at `https://glovebox.cosgriff.io` by Cloudflare Pages (output directory: `site`,
no build command).

- `.well-known/assetlinks.json`: the release signing key's fingerprint for `io.cosgriff.glovebox`, for
  [Privacy Guides' verified apps](https://github.com/privacyguides/verified-apps). Its relation,
  `delegate_permission/org.privacyguides.verifiedapps`, does nothing else: it doesn't make the app open
  links. It must stay in step with the release key, which never changes.
- `obtainium/index.html`: the page the README's "Get it on Obtainium" badge opens. Its button is an
  `obtainium://app/…` link with Glovebox's settings, which GitHub strips from a README. It is used in
  place of Obtainium's own redirect page, which carries analytics and an ad script. No script, no
  third-party requests.

The badge image, `docs/images/badge_obtainium.png`, is Obtainium's own (GPL-3.0, from its
`assets/graphics`), trimmed of its transparent padding.

After a deploy, `curl -i https://glovebox.cosgriff.io/.well-known/assetlinks.json` should answer 200
with this JSON and no redirect, and `https://glovebox.cosgriff.io/obtainium/` should show the page.

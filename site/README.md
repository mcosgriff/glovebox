# glovebox.cosgriff.io

Static files served at `https://glovebox.cosgriff.io` by Cloudflare Pages (output directory: `site`,
no build command).

- `.well-known/assetlinks.json`: the release signing key's fingerprint for `io.cosgriff.glovebox`, for
  [Privacy Guides' verified apps](https://github.com/privacyguides/verified-apps). Its relation,
  `delegate_permission/org.privacyguides.verifiedapps`, does nothing else: it doesn't make the app open
  links. It must stay in step with the release key, which never changes.

After a deploy, `curl -i https://glovebox.cosgriff.io/.well-known/assetlinks.json` should answer 200
with this JSON and no redirect.

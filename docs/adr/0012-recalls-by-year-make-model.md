# Recalls are looked up by year/make/model, and the user marks them done

NHTSA's public recall API answers by year/make/model. It can't confirm whether a particular VIN is affected or has been repaired, so the app lists the model's recalls, the user marks one done (or archives it as not applying), and the app links to the maker's own VIN lookup for the definitive answer.

## Considered Options

Written down after the decision, not at the time; recorded so they aren't proposed again unexamined.

- **The makers' own VIN recall lookups.** They answer for one VIN, repairs included, but they are other hosts, one per maker, which breaks [ADR-0007](0007-network-nhtsa-only.md). The app links to them instead, so the user's browser makes that request.
- **Scraping NHTSA's VIN lookup page.** It is a web page, not a public API, and it can change without notice.

## Consequences

- The list can include recalls that don't apply to this vehicle, which is why archiving exists.
- NHTSA files some recalls under other model names than the VIN decode gives (#460).

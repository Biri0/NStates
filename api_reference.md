# NationStates API Reference

Base URL: `https://www.nationstates.net/cgi-bin/api.cgi`

## Authentication

All private shards/commands require HTTPS and one or more of these headers:

| Header | Format | Description |
|--------|--------|-------------|
| `X-Password` | string (e.g. `hunter2`) | Nation's plaintext password |
| `X-Autologin` | string (e.g. `rjK,CEu9b6T5A45zkvHtA`) | Encrypted password, returned in response header when authenticating with X-Password. Valid until password changes. |
| `X-Pin` | integer (e.g. `1234567890`) | Session PIN, returned in response header when authenticating with X-Password or X-Autologin. Valid for 2 hours or until logout. |

### Auth Flow

1. First request: use `X-Password` -> server returns `X-Pin` and `X-Autologin` in response headers
2. Subsequent requests: use `X-Pin` (fast, avoids 409 Conflict errors)
3. For persistent storage: save `X-Autologin` (avoids storing plaintext password)
4. **409 Conflict**: triggered when logging in (X-Password/X-Autologin) too frequently (within seconds). Use X-Pin to avoid.
5. Logging in cancels any existing session (invalidates previous Pin)

### Mandatory: User-Agent

Every request must include a `User-Agent` header identifying your script (URL or email). Returns 403 if missing.

---

## Rate Limits

- **50 requests per 30 seconds** (flush bucket)
- Response headers:
  - `RateLimit-Limit`: total allowed (50)
  - `RateLimit-Remaining`: remaining in current window
  - `RateLimit-Reset`: seconds until window resets
  - `Retry-After`: seconds to wait when blocked (status 429)

---

## Nation API

### Standard

```
GET ?nation={name}
```

Returns a compendium of commonly sought info. Use shards if you need specific data.

### Public Shards

```
GET ?nation={name}&q={shard1}+{shard2}+...
```

| Shard | Notes |
|-------|-------|
| `admirable` | |
| `admirables` | |
| `animal` | |
| `animaltrait` | |
| `answered` | |
| `banner` | One Rift banner code (primary or random eligible). Convert: `/images/banners/{code}.jpg` |
| `banners` | All Rift banners (primary first, rest random order) |
| `capital` | |
| `category` | |
| `census` | See census options below |
| `crime` | |
| `currency` | |
| `customleader` | |
| `customcapital` | |
| `customreligion` | |
| `dbid` | |
| `deaths` | |
| `demonym` | |
| `demonym2` | |
| `demonym2plural` | |
| `dispatches` | |
| `dispatchlist` | |
| `endorsements` | |
| `factbooks` | |
| `factbooklist` | |
| `firstlogin` | |
| `flag` | |
| `founded` | |
| `foundedtime` | |
| `freedom` | |
| `fullname` | |
| `gavote` | |
| `gdp` | |
| `govt` | |
| `govtdesc` | |
| `govtpriority` | |
| `happenings` | |
| `income` | |
| `industrydesc` | |
| `influence` | |
| `influencenum` | |
| `lastactivity` | |
| `lastlogin` | |
| `leader` | |
| `legislation` | |
| `majorindustry` | |
| `motto` | |
| `name` | |
| `notable` | |
| `notables` | |
| `nstats` | |
| `policies` | |
| `poorest` | |
| `population` | |
| `publicsector` | |
| `rcensus` | |
| `region` | |
| `religion` | |
| `richest` | |
| `scvote` | |
| `sectors` | |
| `sensibilities` | |
| `tax` | |
| `tgcanrecruit` | Optional: `from={region_name}`. Returns 1/0. |
| `tgcancampaign` | Returns 1/0. |
| `type` | |
| `wa` | |
| `wabadges` | |
| `wcensus` | |
| `zombie` | Z-Day only (October 31) |

#### Census Options

```
GET ?nation={name}&q=census&scale={id1}+{id2}&mode={mode1}+{mode2}
```

- `scale`: Census scale IDs (or `all`)
- `mode`: `score`, `rank`, `rrank`, `prank`, `prrank`
- `mode=history`: Special mode (only scores, no ranks). Optional: `from={timestamp}&to={timestamp}`

### Private Shards (require authentication)

```
GET ?nation={name}&q={shard}
Headers: X-Password / X-Autologin / X-Pin
```

| Shard | Notes |
|-------|-------|
| `dossier` | List of nations in dossier |
| `issues` | Current issues with full text and options |
| `issuesummary` | Summary of issues |
| `nextissue` | Next issue details |
| `nextissuetime` | Timestamp of next issue |
| `notices` | Unread notices + notices < 48h. Optional: `from={timestamp}` for older |
| `packs` | Trading card packs |
| `ping` | Registers a login (prevents nation from CTE due to inactivity) |
| `rdossier` | List of regions in dossier |
| `unread` | Unread counts (telegrams, notices, etc.) |

### Private Commands (require authentication)

Use `c={command}` instead of `q={shard}`. Most require prepare/execute two-step process.

#### `issue` - Address an Issue

```
POST ?nation={name}&c=issue&issue={number}&option={number}
```

- `option=-1` to dismiss
- Option IDs start at 0
- Does NOT require prepare/execute (single step)
- Returns: `OK`, `ERROR`, `DESC`, `RANKINGS`, `UNLOCKS`, `RECLASSIFICATIONS`, `NEW_POLICIES`, `REMOVED_POLICIES`

#### `giftcard` - Gift a Trading Card

```
POST ?nation={name}&c=giftcard&cardid={number}&season={number}&to={nation_name}&mode=prepare
POST ?nation={name}&c=giftcard&cardid={number}&season={number}&to={nation_name}&mode=execute&token={token}
```

#### `junkcard` - Junk a Trading Card

```
POST ?nation={name}&c=junkcard&cardid={number}&season={number}&mode=prepare
POST ?nation={name}&c=junkcard&cardid={number}&season={number}&mode=execute&token={token}
```

#### `dispatch` - Write/Edit/Remove a Dispatch

Action: `add`, `edit`, `remove`

```
# Add
POST ?nation={name}&c=dispatch&dispatch=add&title={text}&text={text}&category={number}&subcategory={number}&mode=prepare
POST ?nation={name}&c=dispatch&dispatch=add&title={text}&text={text}&category={number}&subcategory={number}&mode=execute&token={token}

# Edit
POST ?nation={name}&c=dispatch&dispatch=edit&dispatchid={number}&title={text}&text={text}&category={number}&subcategory={number}&mode=prepare
POST ?nation={name}&c=dispatch&dispatch=edit&dispatchid={number}&title={text}&text={text}&category={number}&subcategory={number}&mode=execute&token={token}

# Remove
POST ?nation={name}&c=dispatch&dispatch=remove&dispatchid={number}&mode=prepare
POST ?nation={name}&c=dispatch&dispatch=remove&dispatchid={number}&mode=execute&token={token}
```

#### `rmbpost` - Post to Regional Message Board

```
POST ?nation={name}&region={region}&c=rmbpost&text={text}&mode=prepare
POST ?nation={name}&region={region}&c=rmbpost&text={text}&mode=execute&token={token}
```

### Prepare & Execute (Two-Step) Flow

1. Send request with `mode=prepare` -> server returns `token`
2. Re-send same request with `mode=execute&token={token}`
3. Server rejects duplicate tokens (must prepare fresh each time)
4. Use `X-Pin` for the execute step (since X-Password/X-Autologin cause login, which conflicts if done in quick succession)

---

## Region API

### Standard

```
GET ?region={name}
```

### Shards

```
GET ?region={name}&q={shard1}+{shard2}+...
```

| Shard | Notes |
|-------|-------|
| `banlist` | |
| `banner` | |
| `bannerby` | |
| `bannerurl` | |
| `census` | Same options as nation census |
| `censusranks` | With `scale={id}` |
| `dbid` | |
| `delegate` | |
| `delegateauth` | Authority letter codes (see below) |
| `delegatevotes` | |
| `dispatches` | |
| `embassies` | |
| `embassyrmb` | Posting policy: `0`=none, `con`=delegates+founders, `off`=officers, `com`=comms officers, `all`=all |
| `factbook` | |
| `flag` | |
| `founded` | |
| `foundedtime` | |
| `founder` | |
| `frontier` | |
| `gavote` | |
| `governor` | |
| `governortitle` | |
| `happenings` | |
| `history` | |
| `lastupdate` | |
| `lastmajorupdate` | |
| `lastminorupdate` | |
| `magnetism` | |
| `messages` | RMB messages. Options: `limit=1-100`, `offset={n}`, `fromid={post_id}` |
| `name` | |
| `nations` | |
| `numnations` | |
| `wanations` | |
| `numwanations` | |
| `officers` | |
| `poll` | |
| `power` | |
| `recruiters` | |
| `scvote` | |
| `tags` | |
| `wabadges` | |
| `zombie` | Z-Day only |

### Authority Codes

| Code | Authority |
|------|-----------|
| X | Executive |
| W | World Assembly |
| S | Succession |
| A | Appearance |
| B | Border Control |
| C | Communications |
| E | Embassies |
| P | Polls |

### RMB Message Status Codes

| Code | Meaning |
|------|---------|
| 0 | Regular post |
| 1 | Suppressed but viewable (has SUPPRESSOR field) |
| 2 | Deleted by author (not viewable) |
| 9 | Suppressed by moderator (not viewable) |

---

## World API

```
GET ?q={shard1}+{shard2}+...
```

| Shard | Notes |
|-------|-------|
| `banner` | With `banner={id1},{id2},...` |
| `census` | Same options as nation census |
| `censusid` | |
| `censusdesc` | Optional: `scale={id}` |
| `censusname` | Optional: `scale={id}` |
| `censusranks` | Optional: `scale={id}&start={rank}` |
| `censusscale` | Optional: `scale={id}` |
| `censustitle` | Optional: `scale={id}` |
| `dispatch` | With `dispatchid={id}` |
| `dispatchlist` | Options: `dispatchauthor`, `dispatchcategory`, `dispatchsort` (new/best) |
| `faction` | N-Day only. With `id={id}` |
| `factions` | N-Day only |
| `featuredregion` | |
| `happenings` | See happenings options below |
| `lasteventid` | |
| `nations` | |
| `newnations` | |
| `newnationdetails` | |
| `numnations` | |
| `numregions` | |
| `poll` | With `pollid={id}` |
| `regions` | |
| `regionsbytag` | With `tags={tag1},{tag2},...` (max 10, prefix `-` to negate) |
| `tgqueue` | |

### Happenings Options

- `view=nation.{name}` or `view=region.{name}` (comma-separated for multiple)
- `filter={type1}+{type2}`: `law`, `change`, `dispatch`, `rmb`, `embassy`, `eject`, `admin`, `move`, `founding`, `cte`, `vote`, `resolution`, `member`, `endo`
- `limit={number}`
- `sinceid={number}` / `beforeid={number}`
- `sincetime={timestamp}` / `beforetime={timestamp}`
- **28-second delay** between event and API visibility

---

## World Assembly API

```
GET ?wa={council_id}&q={shard1}+{shard2}+...
```

`council_id`: 1 = General Assembly, 2 = Security Council

| Shard | Notes |
|-------|-------|
| `numnations` | |
| `numdelegates` | |
| `delegates` | |
| `members` | |
| `happenings` | |
| `proposals` | |
| `resolution` | Optional: `id={resolution_id}` for specific resolution |
| `voters` | Must be used with `resolution` (current at-vote only) |
| `votetrack` | Must be used with `resolution` (current at-vote only) |
| `dellog` | Must be used with `resolution` (current at-vote only) |
| `delvotes` | Must be used with `resolution` (current at-vote only) |
| `lastresolution` | |

---

## Telegrams API

Requires an **API Client Key** (obtained from moderators via Help Request).

```
GET ?a=sendTG&client={client_key}&tgid={tgid}&key={secret_key}&to={nation_name}
```

### TG Rate Limits

- Recruitment TGs: 1 per **180 seconds**
- Non-recruitment TGs: 1 per **30 seconds**
- Returns `X-Retry-After` header on failure

---

## Trading Cards API

### Individual Card

```
GET ?q=card+{shard1}+{shard2};cardid={id};season={season}
```

Shards: (none), `info`, `markets`, `owners`, `trades`

`trades` options: `limit`, `sincetime`, `beforetime`

### Deck

```
GET ?q=cards+deck;nationname={name}
GET ?q=cards+deck;nationid={id}
```

### Deck Owner Info

```
GET ?q=cards+info;nationname={name}
GET ?q=cards+info;nationid={id}
```

### Asks and Bids

```
GET ?q=cards+asksbids;nationname={name}
```

### Collections

```
GET ?q=cards+collections;nationname={name}
GET ?q=cards+collection;collectionid={id}
```

### Auctions

```
GET ?q=cards+auctions
```

### All Trades

```
GET ?q=cards+trades
```

Options: `limit`, `sincetime`, `beforetime`

---

## Verification API

```
GET ?a=verify&nation={name}&checksum={code}
```

Optional: `&token={your_token}` for site-specific verification.

Returns `1` (valid) or `0` (invalid).

Can combine with shards: `&q={shard1}+{shard2}` -> verification result in `<VERIFY>` tag.

---

## Server-Sent Events (SSE)

```
GET https://www.nationstates.net/api/{bucket1}+{bucket2}+...
```

Buckets: `law`, `change`, `dispatch`, `rmb`, `embassy`, `eject`, `admin`, `move`, `founding`, `cte`, `vote`, `resolution`, `member`, `endo`, `nation:{name}`, `region:{name}`

Event format (JSON): `id`, `time`, `str`

Limits: 5 concurrent connections per IP. Returns 429 if exceeded.

---

## API Versioning

Append `&v={version}` to lock to a specific API version. Two most recent versions supported.

```
GET ?nation={name}&v=12
```

Check current version: `GET ?a=version`

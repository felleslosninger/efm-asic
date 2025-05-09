# Changelog


# Java 21 releases

# 1.0.1

* ZipBomb protection was upped to 1 GiB pr META-INF entry (see [MOVE-3770](https://digdir.atlassian.net/browse/MOVE-3770))

# 1.0.0

* Java 21
* Jakarta EE for XML binding
* ZipBomb protection (limits at 1 MiB pr META-INF entry)

# Older Java 8 releases

```
Coordinates                           Last updated
===========                           ============
no.difi.commons:commons-asic:0.12.0   19 Oct 2023 at 12:46 (CEST)
no.difi.commons:commons-asic:0.11.0   25 Mar 2022 at 08:49 (CET)
no.difi.commons:commons-asic:0.10.0   07 Dec 2021 at 12:10 (CET)
no.difi.commons:commons-asic:0.9.5    09 Nov 2021 at 09:46 (CET)
no.difi.commons:commons-asic:0.9.4    30 Jun 2021 at 10:05 (CEST)
no.difi.commons:commons-asic:0.9.3    25 Jul 2017 at 14:13 (CEST)
no.difi.commons:commons-asic:0.9.2    07 Apr 2016 at 14:26 (CEST)
no.difi.commons:commons-asic:0.9.1    31 Jul 2015 at 17:04 (CEST)
no.difi.commons:commons-asic:0.9.0    27 Jul 2015 at 20:15 (CEST)
```

## 0.9.2

* Added wrappers for encryption and decryption content on-fly.
* Remove dependencies commons-codec (now test only) and commons-io, adding Guava.
* Some refactoring.

## 0.9.1

* Some refactoring.

## 0.9.0

* Initial release.
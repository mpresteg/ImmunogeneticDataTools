# hla-report-extraction (early stage — text extraction + candidate-line detection for all 3 labs, no GL String construction yet)

## Purpose

Extract structured HLA typing results — as GL Strings — from lab report PDFs.
This closes a real gap in the reactor: `ld-validation` analyzes GL Strings once
you have them, and `ld-service` exposes that analysis over REST and a browser
UI, but nothing here gets you *from* "a PDF a lab sent" *to* a GL String in the
first place. That extraction step is this module's entire scope.

## Trying it yourself

No REST API or browser UI yet (unlike `ld-service`) — this module isn't at
that maturity. What exists today is a minimal CLI for manually trying a real
PDF against whatever candidate-line detectors currently exist. Run both
commands below from the repo root (the PDF argument resolves relative to
wherever you run the command from, not relative to this module directory —
this one points at a real fixture so it's copy-pasteable as-is):

```
mvn -pl hla-report-extraction package
hla-report-extraction/target/appassembler/bin/detect-hla-report hla-report-extraction/src/test/resources/sample-reports/versiti-hla-c-high-resolution-sample.pdf
```

Swap in the path to any PDF you want to try — one of your own, or either of
the other two fixtures under `src/test/resources/sample-reports/`.

Prints each detector's candidates as a review worklist (source line number +
exact report text alongside each one) — **not** a GL String, not validated
typing data. Right now that's eight detectors: `cegat`/#43, `versiti`/#42,
and six Histogenetics ones (`histogenetics-page1`/#49,
`histogenetics-appendix`/#50, `histogenetics-failed`/`histogenetics-pending`/
`histogenetics-xxxx`/`histogenetics-narrative-ambiguity`, all #51). Running
it against a report shape a given detector doesn't recognize is expected to
print 0 candidates for that detector, not an error — that's not a bug, it
just means nothing here
recognizes that report's shape yet (see "How tethered is this to the 3 known
reports?" below).

On macOS, if `JAVA_HOME` isn't set you may see a harmless
`Unable to locate a Java Runtime` line before the real output — the
generated script still finds a real JDK on `PATH` and runs correctly
afterward; this is a pre-existing `appassembler-maven-plugin` quirk shared
with `ld-tools`' generated scripts, nothing specific to this module.

## Relationship to the rest of this repo

- **No new GL String grammar.** `ld-validation`'s `GLStringUtilities` /
  `GLStringConstants` (`org.dash.valid.gl`) already define and construct GL
  Strings correctly. This module's job is getting from a PDF's raw, messy text
  to inputs for that existing code — not reimplementing GL String syntax.
- **Downstream is already solved.** Once this module produces a GL String,
  `ld-validation`/`ld-tools`/`ld-service` already know what to do with it. This
  module's output contract is simply: a GL String, plus an explicit human
  review/confirmation step before it's treated as trustworthy input to those.
- **No separate reference-data sourcing needed.** The IMGT/HLA database
  version is already a pinned, versioned dependency elsewhere in this repo
  (`org.dash.hladb`, currently `3.65.0`) and this module reuses it as-is.

## Guiding principles

- **Structural signal, not a content guess.** When extracted text is
  ambiguous, surface it for human review rather than silently interpreting or
  "correcting" it. A missed extraction is a much smaller problem than a wrong
  one silently fed into a GL String.
- **Real reports drive the grammar, not assumption.** Extraction rules get
  written against actual report text, not a guess at what a "typical" report
  might look like. See "Current status" below.
- **OCR-sourced text needs *more* scrutiny than a real text layer, not the
  same amount.** Prefer a PDF's embedded text layer; fall back to OCR only
  when that layer is effectively absent (a scanned page), and flag
  OCR-derived content distinctly rather than treating it as equally reliable.
- **No demographic/PHI auto-population without explicit human review.**
  Whatever this eventually feeds (patient/subject context for a downstream
  consumer) gets surfaced for confirmation, never silently populated from
  extracted text.

## Current status

Three real, de-identified sample reports obtained, from three different
labs. See `src/test/resources/sample-reports/PROVENANCE.md` for each
source; all three are safe to use as test fixtures (placeholder or blank
identifying fields, no recoverable PHI — verified directly against each
file's text layer, not just visually).

- **Versiti Wisconsin** — test 91500, HLA-C High Resolution. Locus result
  reported as two allele calls; the second at the G-group level, resolved
  via an `R1` footnote reference to the underlying ambiguous alleles
  (`C*07:04:01G = HLA-C*07:04:01G=C*07:04/11`). A naive "read the row,
  done" parser would silently drop that ambiguity.
- **CeGaT** — HLA class I and II typing. The "easy case": every locus
  reported as two fully-resolved alleles (or one, for a homozygous or
  single-gene-present locus like DRB345), no G-codes, no footnotes, no
  ambiguity at all.
- **Histogenetics** — an 11-page patient + two-donor bone marrow/stem cell
  matching report, the richest of the three. Reports **G-codes as the
  primary result** (e.g. `A* 02:01:01G`), not resolved alleles — closer to
  GL-String's own ambiguity model than either other report. Includes a
  donor-matching ratio (`10/10 Matched`), a `Null Allele Resolution Status`
  footnote per sample (specific null alleles explicitly excluded from a
  G-code call), an NMDP-code fallback when G-code resolution isn't
  available (`DQB1*02:DKCVG`), and a full appendix expanding every G-code
  to its NMDP allele code and complete enumerated list of included alleles.
  Also has a page-numbering quirk worth remembering: the PDF's physical
  page 2 prints footer "Page 3 of 14" — physical page index and printed
  page number diverge, so anything anchored to a printed "Page X of Y"
  string needs to not assume it lines up with `PDDocument` page indices.

Step 1 only so far: `PdfTextExtractor` pulls the embedded text layer out of
a report PDF, tested against all three samples (`PdfTextExtractorTest`
covers each, plus the multi-page Histogenetics appendix specifically, to
confirm extraction isn't just reading page 1). It does no interpretation of
the text — no locus/allele recognition, no line classification. That's
deliberate: "did we read the PDF correctly" and "did we understand what it
says" are being kept as independently testable, separately reviewable
concerns (see Guiding principles above).

These three already show genuinely different ambiguity-representation
strategies — none, footnote-resolved, and G-code-as-primary-result — which
is exactly why one sample wasn't enough to design candidate-line-detection
against. **Still want more real, de-identified samples** — every additional
lab's convention narrows the gap between "a design that handles the samples
seen so far" and "a design that generalizes."

### How tethered is this to the 3 known reports?

Honestly: quite. Worth being precise about what's actually general versus
what's report-specific, since it's not uniform:

- **Genuinely general:** `PdfTextExtractor` (no report-specific
  assumptions), `AlleleToken` (the `LOCUS*field:field...` shape is standard
  HLA nomenclature, not a lab-ism), `LocusResultCandidate` (a plain data
  carrier), and the overall extract → detect → human-reviewed-candidate →
  GL-String pipeline shape.
- **Tightly tethered:** each detector itself. `CegatLocusResultLineDetector`
  (#43) recognizes exactly one row shape — bare locus label, then 1-2
  allele tokens, nothing else on the line, whitespace-separated. It would
  **not** detect a row with a comma between alleles, a row with extra
  trailing text (an annotation column, an inline zygosity note), or
  serological notation (`A2, B7`) instead of star-allele notation — the
  original design doc flagged that last one as a real possibility, and it's
  still entirely unhandled by anything in this module. Even a different
  *template revision* of the same CeGaT report could reflow the table
  slightly and break this exact detector.

So: one lab format in, one detector out, by design (each detector is built
against real text it's actually been tested against, not a guess at "what a
typical report looks like" — see "Real reports drive the grammar, not
assumption" above). That's deliberate, but it doesn't scale forever: if a
bespoke detector is still the answer after 6-8 more labs with nothing
generalizing, that's the point to seriously consider a more general,
configurable table-shape matcher (or genuine structural layout analysis)
instead of continuing to hand-write one class per lab. Revisiting this
periodically as real samples accumulate, rather than deciding it now with
too little data to know which parts would actually generalize.

On **VLM / OCR / managed document-AI (e.g. Textract, Document Intelligence,
Bedrock Data Automation)** as alternatives to text-layer extraction: not
used here, deliberately. These reports carry real patient PHI, and sending
page images to a third-party cloud API means that PHI leaves the local
machine — not something this library can assume every downstream user has a
BAA in place for. A VLM's output is also a probabilistic guess dressed up as
structured data, which cuts against "structural signal, not a content
guess" above: harder to make it fail loud on an ambiguous case like
Versiti's `R1` footnote or Histogenetics' null-allele exclusions, versus a
rule-based parser that either matches a known pattern or doesn't. All three
samples obtained so far have a real text layer, so there's been nothing to
actually test OCR against yet either — text-layer extraction alone has
sufficed.

## Then (tentative — more real report samples will refine or replace this)

Tracked as GitHub issues on this fork now that step 1 made the remaining
steps concrete enough to decompose (previously just this list): see
[#42](https://github.com/mpresteg/ImmunogeneticDataTools/issues/42),
[#43](https://github.com/mpresteg/ImmunogeneticDataTools/issues/43),
[#44](https://github.com/mpresteg/ImmunogeneticDataTools/issues/44),
[#45](https://github.com/mpresteg/ImmunogeneticDataTools/issues/45),
[#46](https://github.com/mpresteg/ImmunogeneticDataTools/issues/46). Keep
this list and those issues in sync as work lands — don't let this become
the stale copy.

1. ~~PDF text-layer extraction~~ — done (`PdfTextExtractor`), with an OCR
   fallback for scanned pages still to come once a scanned sample exists to
   test one against. Prefer the embedded text layer; OCR only when it's
   effectively empty; OCR-derived candidates flagged for extra scrutiny.
2. Candidate-line detection for HLA-typing-shaped content, surfaced for
   review — never auto-parsed straight into a GL String. Needs to handle,
   as first-class cases rather than afterthoughts: Versiti's
   footnote-reference pattern (locus row → `R1` marker → footnote
   resolving the ambiguity, ~~issue #42~~ — done, see below), CeGaT's
   fully-resolved no-ambiguity case (~~issue #43~~ — done, see below), and
   Histogenetics' G-code-as-primary-result plus its appendix's
   G-code-to-included-alleles expansion and null-allele exclusions
   (issue #44, split into #48/#49/#50/#51 once work started — see below).

   **#43 done:** `CegatLocusResultLineDetector` detects CeGaT's
   `LOCUS ALLELE1 [ALLELE2]` row shape, producing a `LocusResultCandidate`
   per locus (never a GL String directly — see "Guiding principles"
   above). Two things worth calling out:
   - It cross-checks that each allele token's own locus prefix agrees
     with the row's declared locus (accounting for DRB345's combined-locus
     special case, where the real prefix is whichever of DRB3/4/5 is
     actually present) — turning "looks allele-shaped" into "is
     internally consistent with the row it's on," a stronger structural
     signal than a bare shape match.
   - It's shape-driven, not section-scoped — it doesn't look for CeGaT's
     "Results" header first. Confirmed safe against all three known
     reports (`CegatLocusResultLineDetectorTest` includes negative cases
     against Versiti's and Histogenetics' differently-shaped reports,
     zero false positives), but a future report could in principle
     produce a same-shaped false positive elsewhere on the page — exactly
     why this produces review candidates, not trusted output.

   **#42 done:** `VersitiLocusResultLineDetector` detects the header +
   optional continuation line, follows any footnote marker (e.g. `R1`) to
   its resolution elsewhere in the document, and attaches the resolved
   text to the candidate — never just the bare G-group code. Produces a
   distinct `VersitiLocusResultCandidate`/`FootnoteReferencedAlleleCall`
   shape rather than reusing `LocusResultCandidate`: Versiti's report
   carries real structure (per-allele-call footnote resolution, a
   resolution-level descriptor like "High") CeGaT's simpler row has no
   equivalent for, and forcing one shared candidate shape onto both with
   only one real sample of each would be guessing at what should
   generalize before there's evidence to know. Two things worth calling
   out:
   - A footnote marker seen with no matching definition found anywhere in
     the document is surfaced as *unresolved* (not silently treated as if
     there had been no marker at all) — `hasUnresolvedFootnoteReference()`
     on both the allele call and the candidate. Exercised with a synthetic
     case (`VersitiLocusResultLineDetectorTest`) since no real sample has
     hit this yet.
   - `LocusLookup`, a shared silent locus-shortName lookup, got factored
     out of `CegatLocusResultLineDetector` once this detector needed the
     identical lookup — the first real sign of what two detectors
     genuinely have in common versus what's still per-lab (see "How
     tethered is this to the 3 known reports?" above).

   **#44 turned out bigger than scoped, split into #48/#49/#50/#51 once
   work actually started** — the fixture PDF is genuinely 4 separate
   reports bundled together, not one: a "Complete"-status patient+2-donors
   report (the original scope), plus a **FAILED** typing (insufficient
   DNA), a **PENDING** typing, and one using `XXXX` placeholders with
   ambiguity expressed as narrative prose in the Report History section
   instead of a table at all (e.g.
   `Possible Allele in A : 01:01:01/02:01:01/30:01:01.`) — a fourth
   ambiguity-representation convention, on top of the three the module
   README already tracked. This is exactly the kind of thing "real reports
   drive the grammar, not assumption" is for: none of this was visible
   until actually reading the whole document closely enough to build
   against it.

   **#48 done:** `HistogeneticsNoiseFilter` strips the per-page boilerplate
   (watermark fragments, letterhead, footer, CONFIDENTIAL/DISCLAIMER
   notices, repeated report title, repeated appendix column headers)
   before any candidate-line detection runs. Confirmed this isn't just
   cosmetic: a single appendix G-code's `Included Alleles` list can be
   interrupted **mid-stream** by ~17 lines of this boilerplate at a page
   break, and the filter's whole job is turning the two halves back into
   adjacent lines so #50's planned accumulator can treat "keep consuming
   until the next line looks like a new locus row" as actually sufficient,
   rather than needing to understand every boilerplate category itself.
   Also confirmed the watermark's letter-grouping isn't perfectly
   consistent across every occurrence (one instance splits differently
   than the rest) — matched by shape (short, all-caps, letters-only line)
   per the issue's own suggested heuristic, not by memorizing the literal
   fragment list. New `org.nmdp.hlareport.candidate.histogenetics`
   subpackage, since Histogenetics needs several distinct pieces
   (filter, page-1 table detector, appendix accumulator, non-standard-state
   handling) where CeGaT/Versiti each only needed one class.

   **#50 done:** `HistogeneticsAppendixAccumulator` reassembles each
   appendix row's `Included Alleles` list across however many physical
   lines it spans (up to ~39 for the longest one, including a mid-list page
   break), attributing each row to the correct sample (patient/donor 1/
   donor 2) via the nearest preceding `Sample ID :` marker. Confirmed
   against the real fixture: 30 entries (10 rows × 3 samples), not
   deduplicated even though all 3 samples' content is identical here — per
   the issue's own note not to assume repeats always match. Two things
   worth calling out:
   - `HistogeneticsNoiseFilter` (#48) got a small follow-up refactor here:
     it now returns `NumberedLine` (text + original line number) instead
     of plain `String`, since this accumulator needed real traceability
     back to the source document — filtering discards line-number
     continuity, and without carrying original numbers along, a candidate
     class would either lose that traceability or have to re-derive it.
     Confirmed the numbers survive correctly: the two halves of the
     page-break-split list from #48's own test are original lines 139 and
     160, not adjacent originally, even though they're adjacent
     post-filtering.
   - `isGCode()` distinguishes a real G-code (e.g. `02:01:01G`, ends in
     digit+`G`) from an NMDP-code fallback that happens to also end in
     `G` (e.g. `02:DKCVG`, ends in letter+`G`) — confirmed against the
     real `DQB1*02:DKCVG` row, which is the fallback case per the report's
     own note #4 (no G-code was available, so the reported value *is* the
     NMDP code, duplicated in both columns).

   **#49 done:** `HistogeneticsPageOneTableDetector` detects the page-1
   summary table -- a header row of locus labels followed by exactly 2
   data rows whose values line up with the header BY COLUMN POSITION, a
   genuinely different shape from every other detector in this module
   (all of which read one locus per line). Produces one
   `HistogeneticsSampleResultCandidate` per sample block (patient, donor 1,
   donor 2), not one per locus like CeGaT/Versiti — the source document
   itself binds a sample's loci together in one 3-line table, and
   `Matching Ratio`/`Null Allele Resolution Status` are properties of the
   whole sample, not any single locus. Confirmed against the real fixture:
   exactly 3 candidates (not 6) — the FAILED, PENDING, and
   XXXX/narrative-ambiguity reports each correctly produce zero. Two
   things worth calling out:
   - **A real trap, caught by checking actual data before trusting a
     field**: the report's own `Typing Status :` field is NOT a reliable
     success signal — it reads `Complete` on the FAILED and
     XXXX/narrative-ambiguity reports too (confirmed directly in the
     extracted text), apparently meaning "this report was finalized," not
     "typing succeeded." Detection instead checks whether each locus's
     actual reported value is code-shaped
     (`HistogeneticsCode.isCodeShaped()`) — true for a G-code or NMDP-code,
     false for `NA`/`FAILED`/`PENDING`/`XXXX`, all of which happen to never
     start with a digit. `Typing Status` is still captured on the
     candidate, for transparency, just not trusted.
   - `HistogeneticsCode`, a shared G-code/NMDP-code shape check, got
     factored out of `HistogeneticsAppendixAccumulator` (#50) once this
     detector needed the identical check — another genuine sign of what
     Histogenetics' own sub-detectors have in common, alongside
     `LocusLookup` (shared across labs) and `HistogeneticsNoiseFilter`
     (shared across all Histogenetics detection).

   **#51 done — and all four of #44's sub-issues are now complete.** The
   fixture's 3 non-"Complete" bundled reports (FAILED, PENDING, and one
   using `XXXX` placeholders with narrative-ambiguity prose) are surfaced
   distinctly rather than silently producing zero candidates the way an
   unrecognized report shape correctly does — a FAILED or PENDING result
   is real information a reviewer needs to see, not indistinguishable from
   "nothing here understood this report."
   - `HistogeneticsPlaceholderTableDetector` handles FAILED, PENDING, *and*
     XXXX with one class, parameterized by which placeholder word to look
     for — confirmed against the real fixture that all three use the
     *exact same* table shape `HistogeneticsPageOneTableDetector` reads for
     real G-codes, just with a different literal value in every cell. The
     issue itself guessed "likely three genuinely separate
     detectors/candidate types" going in; real data showed the table
     portion of all three collapses into one, with only the narrative
     piece below needing something different — three real confirmations
     of one shape is what justified generalizing here, not a guess
     extended from one sample.
   - `HistogeneticsSampleBlockHeader` (the `"Histo ID :"` marker and the
     9-column locus-header-row parsing) got factored out of
     `HistogeneticsPageOneTableDetector` once this detector needed the
     identical logic — the same discipline as `HistogeneticsCode` and
     `LocusLookup` before it.
   - `HistogeneticsNarrativeAmbiguityDetector` is a genuinely different
     kind of detector from everything else in this module: it parses
     prose (`"Possible Allele in A : 01:01:01/02:01:01/30:01:01."` in the
     report's Report History section), not a table row or column. A fifth
     distinct ambiguity-representation convention, on top of the four the
     module README already tracked — bare slash-separated allele
     designations with no locus prefix at all, embedded in a sentence
     that (for its first occurrence) shares a physical line with a date
     and a differently-cased `"Sample Id :"` marker (not the appendix's
     `"Sample ID :"`, confirmed genuinely different text, not a typo to
     normalize away).
3. Human-reviewed candidates converted to a GL String via the existing
   `GLStringUtilities` (issue #45).

   **CeGaT done.** `CegatGlStringBuilder` builds a full multi-locus GL
   String from `LocusResultCandidate`s — `HLA-` prefixing and joining with
   the existing grammar's own delimiters (`+` within a locus, `^` between
   loci), nothing more. CeGaT was the deliberate starting point (same
   reasoning as issue #43 picking it first for detection): every allele
   call is already a complete, standalone designation, so this needed no
   shorthand interpretation at all. Verified against the real fixture two
   ways, not just this module's own assertions: `GLStringUtilities.
   validateGLStringFormat()` accepts the constructed string, and handing
   it to a real `LinkageDisequilibriumGenotypeList` (ld-validation's own
   class) succeeds and round-trips unchanged — per this repo's
   `CONTRIBUTING.md` ("prefer verifying behavior for real"). A
   single-call locus (the DRB345 case) produces a single-copy fragment
   with no `+` at all, rather than guessing homozygosity by duplicating
   the one known call.

   **Versiti and Histogenetics deliberately not attempted yet** — both
   have a real open question, not just unstarted work:
   - Versiti's footnote shorthand (`C*07:04/11`) has a genuine semantic
     ambiguity this module can't resolve from the report text alone.
     Tested `GLStringUtilities.fullyQualifyGLString()` (the existing
     utility that would seem to handle exactly this) directly against it:
     it produces `HLA-C*07:04/HLA-C*11` — treating `11` as a standalone
     one-field allele group, *not* as shorthand for `C*07:11` sharing the
     `07` first field. Real HLA G-group conventions more commonly share
     leading fields this way, which would make that output wrong, but
     this module has no way to confirm which reading is actually correct
     for this specific footnote without either authoritative reference
     verification or domain input — exactly the kind of thing "structural
     signal, not a content guess" exists to catch. Building GL String
     construction for Versiti on top of an unverified assumption here
     would risk silently encoding the wrong alleles.
   - Histogenetics' G-codes/NMDP-codes remain the promising anchor
     `GLStringUtilities.decodeMAC()` was already built for — but that
     method makes a live network call to `hml.nmdp.org`'s MAC decode API
     (confirmed by reading its implementation, not assumed). Not the same
     privacy concern as the module's stance against VLM/cloud extraction
     (an NMDP allele code carries no PHI, unlike a page image), but a real
     external dependency worth verifying actually works as expected
     before building on it, which hasn't been done yet.
4. A validation gate before a GL String is considered "reviewed and ready"
   (issue #46) — nothing silently guessed or auto-corrected along the way.

Deliberately underspecified beyond step 1 — this is a starting hypothesis,
not a locked design.

# hla-report-extraction (early stage — PDF text extraction only, no parsing yet)

## Purpose

Extract structured HLA typing results — as GL Strings — from lab report PDFs.
This closes a real gap in the reactor: `ld-validation` analyzes GL Strings once
you have them, and `ld-service` exposes that analysis over REST and a browser
UI, but nothing here gets you *from* "a PDF a lab sent" *to* a GL String in the
first place. That extraction step is this module's entire scope.

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
   resolving the ambiguity, issue #42), CeGaT's fully-resolved
   no-ambiguity case (~~issue #43~~ — done, see below), and Histogenetics'
   G-code-as-primary-result plus its appendix's G-code-to-included-alleles
   expansion and null-allele exclusions (issue #44).

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
3. Human-reviewed candidates converted to a GL String via the existing
   `GLStringUtilities` (issue #45) — Histogenetics' G-codes and NMDP
   allele codes are promising anchors here, since
   `GLStringUtilities.decodeMAC()` already exists to decode NMDP-coded
   typings.
4. A validation gate before a GL String is considered "reviewed and ready"
   (issue #46) — nothing silently guessed or auto-corrected along the way.

Deliberately underspecified beyond step 1 — this is a starting hypothesis,
not a locked design.

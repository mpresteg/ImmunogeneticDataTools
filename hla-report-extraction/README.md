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

One real, de-identified, lab-published sample report obtained: Versiti
Wisconsin's official "SAMPLE REPORT" for test 91500 (HLA-C High Resolution).
See `src/test/resources/sample-reports/PROVENANCE.md` for its source; it's
safe to use as a test fixture (placeholder patient name, watermarked SAMPLE
REPORT throughout, not real PHI).

Step 1 only so far: `PdfTextExtractor` pulls the embedded text layer out of
a report PDF, tested against that sample. It does no interpretation of the
text — no locus/allele recognition, no line classification. That's
deliberate: "did we read the PDF correctly" and "did we understand what it
says" are being kept as independently testable, separately reviewable
concerns (see Guiding principles above).

That one sample already surfaced a real structural wrinkle worth designing
around before writing candidate-line-detection logic: this report's locus
result rows can carry a footnote reference (e.g. `C*07:04:01G` marked `R1`)
that resolves, elsewhere on the page, to the underlying ambiguous alleles
(`C*07:04:01G = HLA-C*07:04:01G=C*07:04/11`). A naive "read the row, done"
parser would silently drop that ambiguity. Whether this is a one-lab
convention or a common pattern across labs is exactly the kind of thing more
real samples would confirm or rule out.

**Still need more real, de-identified samples** — ideally from more than one
lab — before generalizing candidate-line-detection or GL String construction
logic beyond this one report's conventions. One sample is enough to start
scaffolding against; it's not enough to design a general parser against.

On **VLM / OCR / managed document-AI (e.g. Textract, Document Intelligence,
Bedrock Data Automation)** as alternatives to text-layer extraction: not
used here, deliberately. These reports carry real patient PHI, and sending
page images to a third-party cloud API means that PHI leaves the local
machine — not something this library can assume every downstream user has a
BAA in place for. A VLM's output is also a probabilistic guess dressed up as
structured data, which cuts against "structural signal, not a content
guess" above: harder to make it fail loud on an ambiguous case like the
`R1` footnote pattern, versus a rule-based parser that either matches a
known pattern or doesn't. The one sample obtained so far has a real text
layer, so there's been nothing to actually test OCR against yet either —
text-layer extraction alone has sufficed.

## Then (tentative — more real report samples will refine or replace this)

1. ~~PDF text-layer extraction~~ — done (`PdfTextExtractor`), with an OCR
   fallback for scanned pages still to come once a scanned sample exists to
   test one against. Prefer the embedded text layer; OCR only when it's
   effectively empty; OCR-derived candidates flagged for extra scrutiny.
2. Candidate-line detection for HLA-typing-shaped content, surfaced for
   review — never auto-parsed straight into a GL String. Needs to handle
   this report's footnote-reference pattern (locus row → `R1` marker →
   footnote resolving the ambiguity) as a first-class case, not an
   afterthought.
3. Human-reviewed candidates converted to a GL String via the existing
   `GLStringUtilities`.
4. A validation gate before a GL String is considered "reviewed and ready" —
   nothing silently guessed or auto-corrected along the way.

Deliberately underspecified beyond step 1 — this is a starting hypothesis,
not a locked design.

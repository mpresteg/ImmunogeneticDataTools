# hla-report-extraction (proposed module — design stage, no code yet)

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
  might look like. See "Immediate next step" below — this is the actual
  blocker on writing any code at all right now.
- **OCR-sourced text needs *more* scrutiny than a real text layer, not the
  same amount.** Prefer a PDF's embedded text layer; fall back to OCR only
  when that layer is effectively absent (a scanned page), and flag
  OCR-derived content distinctly rather than treating it as equally reliable.
- **No demographic/PHI auto-population without explicit human review.**
  Whatever this eventually feeds (patient/subject context for a downstream
  consumer) gets surfaced for confirmation, never silently populated from
  extracted text.

## Immediate next step

Need one or more real, de-identified HLA typing lab report PDFs before
writing any extraction logic. Specifically want to see: how loci/alleles are
labeled, what resolution is reported (2-field vs. 4-field, e.g. `A*02:01` vs.
`A*02:01:01:01`), how ambiguity is represented (multiple-allele strings, "G"
or "P" group notation), and whether reports already express results in
something GL-String-shaped or need real translation from a different
notation (e.g. serological equivalents like "A2", "B7"). Everything past
that is premature.

## Then (tentative — real report samples will refine or replace this)

1. PDF text-layer extraction with an OCR fallback for scanned pages —
   prefer the embedded text layer, OCR only when it's effectively empty,
   OCR-derived candidates flagged for extra scrutiny.
2. Candidate-line detection for HLA-typing-shaped content, surfaced for
   review — never auto-parsed straight into a GL String.
3. Human-reviewed candidates converted to a GL String via the existing
   `GLStringUtilities`.
4. A validation gate before a GL String is considered "reviewed and ready" —
   nothing silently guessed or auto-corrected along the way.

Deliberately underspecified beyond step 1 — this is a starting hypothesis,
not a locked design.

# Sample report provenance

## versiti-hla-c-high-resolution-sample.pdf

Source: [Versiti Wisconsin, Inc.](https://www.versiti.org/) (formerly BloodCenter of
Wisconsin) — publicly published as their official sample report for test 91500
(HLA-C High Resolution), retrieved from
`mayocliniclabs.com/-/media/it-mmfiles/test notifications/4/0/0/91500 sample report
2019-02-04 hla c high resolution.pdf` (Mayo Clinic Laboratories distributes Versiti's
report as a reference for that test in their catalog).

Not real patient data: the patient name field reads `Qual, HLA-C`, a lab-internal
placeholder, and the document is watermarked `SAMPLE REPORT` throughout. Safe to
commit and use as a test fixture.

Retrieved 2026-09-10.

## cegat-hla-typing-sample.pdf

Source: [CeGaT GmbH](https://www.cegat.com/) (Tübingen, Germany) — their published
`CeGaT_HLA_Sample_Report.pdf`, a two-page medical HLA-typing report for HLA class I
and II (locus/allele-pair table across all 9 loci, method notes on page 2).

Not real patient data: patient name field reads `XXX, XX (*DD.MM.YYYY)`, physician is
`Dr. James Public` at `Model Company`, address `MODEL CITY`, report ID
`R9999999999` — an explicit placeholder template. Safe to commit and use as a test
fixture.

Retrieved 2026-09-10.

## histogenetics-hla-typing-g-code-sample.pdf

Source: [Histogenetics](https://www.histogenetics.com/) (Ossining, NY) — a lab
specializing in HLA typing for bone marrow/stem cell registries. 14-page HLA Typing
Report with patient + two donor results (G-code-level typing, matching ratio, null
allele resolution) and a full appendix expanding each reported G-code to its
constituent NMDP allele code and enumerated included alleles.

Identifying fields (Last Name, First Name, Hospital, Physician, Date of Birth, MR#)
are blank in the underlying text layer, not merely visually covered — confirmed via
direct text-layer extraction (`pdftotext`), so no PHI is recoverable from the file.
The Histo ID fields read the literal placeholders `Patient ID`, `Donor ID 1`, and
`Donor ID 2` rather than real identifiers. User-confirmed as representative of
Histogenetics' typical report format, not a redacted real patient report. Safe to
commit and use as a test fixture.

Retrieved 2026-09-10.

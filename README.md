#Implementation Notes

## Punch List
* DONE Bulk Extractor feature import (see email.txt, # headers, entity in 2nd TDF column)
* DONE Make menu items consistent and add pattern save/load actions
* DONE Implement entity type checkboxes (in main menu)
* DONE Uniform color scheme for policy/action in tables and preview
* Move the repository to https://github.com/bitcurator/bitcurator-redact-pdf per our discussion at the beginning of the contract. You should have administrative privileges on that group. Let me know if you run into any issues with this.
* Build documentation for Ubuntu 18.04LTS (e.g. clean build in BitCurator 2.0.6 with default OpenJDK 11) in the README or INSTALL.
* Add a directory with a few test PDFs and relevant sample runs described (briefly) in documentation.
* Make policy while reviewing a document

##Entity Extraction
* FIXED Need progress indicator for PatternFindingWorker created on redact(), E.G. Belgium finding aid
* DONE We can call Spacy with batches of PDF files.
* DONE We can use Stanford NLP batches as well (w/in Java)
* DONE Show progress through the file list.
* NOPE Perhaps run Spacy on individual files, so that we can show progress in file list.

## Redaction
* DONE Redact a chosen single file now.
* Start a the top of the list, i.e. "Begin Redaction".
* When prompted with a pattern match:
  (1) Redact
  (2) Ignore
 * Option to "Remember this choice for pattern X".

## Possible Features
* Display other coordinates (which files) and stats (# instances) for extracted entities.
* User editable extracted entities, e.g. to remove extraneous words/tokens.
* At least one example textual context for the extracted entity, perhaps all..
* DONE Keep making/re-making redaction decisions until you actually redact that PDF.
* DONE Redact the same PDF again to overwrite the previously redacted file.

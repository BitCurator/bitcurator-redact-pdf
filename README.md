#Implementation Notes

##Entity Extraction
* Need progress indicator for PatternFindingWorker created on redact(), E.G. Belgium finding aid
* We can call Spacy with batches of PDF files.
* We can use Stanford NLP batches as well (w/in Java)
* Show progress through the file list.
* Perhaps run Spacy on individual files, so that we can show progress in file list.

## Redaction
* Redact a chosen single file now.
* Start a the top of the list, i.e. "Begin Redaction".
* When prompted with a pattern match:
  (1) Redact
  (2) Ignore
  (3) Redact in rest of file
  (4) Ignore in rest of file
  (5) Redact in all files
  (6) Ignore in all files

## Possible Features
* User editable extracted entities, e.g. to remove extraneous words/tokens.
* At least one example textual context for the extracted entity, perhaps all..
* Display other coordinates (which files) and stats (# instances) for extracted entities.
* Keep making/re-making redaction decisions until you actually redact that PDF.
* Redact the same PDF again to overwrite the previously redacted file.


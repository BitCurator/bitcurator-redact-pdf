![Logo](https://github.com/BitCurator/bitcurator.github.io/blob/master/logos/BitCurator-Basic-400px.png)

# bitcurator-redact-pdf

[![GitHub issues](https://img.shields.io/github/issues/bitcurator/bitcurator-redact-pdf.svg)](https://github.com/bitcurator/bitcurator-redact-pdf/issues)
[![GitHub forks](https://img.shields.io/github/forks/bitcurator/bitcurator-redact-pdf.svg)](https://github.com/bitcurator/bitcurator-redact-pdf/network)

# Overview

BitCurator Redact PDF is a desktop application designed to help you effectively remove personally identifiable information from PDF files. It can work on PDFs files individually or many at once. This tool uses natural language parsing to detect named entities, such as people, places, and organizations, in the text of PDF files. It can also detect text patterns that you supply as regular expressions. Original PDFs are left as is, with the newly redacted copies placed in an output folder within the current directory.

NOTE: BitCurator Redact PDF redacts PDF text areas completely. It replaces the text characters with empty space and covers the relevant page area with a black rectangle, after which the original text is not recoverable. The program does not necessarily detect or redact PDF file properties that are not shown visually, i.e. authorship information that is captured by PDF producing software.

# Building the Software in BitCurator

1. Start BitCurator VM and then open the terminal.
1. At the terminal, clone the project's code repository:
    ```
    $ git clone https://github.com/bitcurator/bitcurator-redact-pdf
    ```
1. Change to the project directory:
    ```
    $ cd bitcurator-redact-pdf
    ```
1. You need to install the Java build tool, Gradle, and the Ghostscript program (with development libraries) which is used to render PDF pages as images:
    ```
    $ sudo apt-get install gradle ghostscript libgs-dev
    ```
1. Restart your BitCurator machine using the shutdown menu. This will add the
programs you have just installed to the default library path.
1. Run the Gradle build to compile and package the software in a single jar file:
    ```
    $ gradle allJar
    ```
1. The software is packaged as a single, all inclusive jar file that you can run using Java. See the instructions under "Running the Software" below. The single jar is located at `build/libs/bitcurator-redact-pdf-all-1.0.jar`
1. You can install this software on other machines by copying the jar file. Java runtime v1.8 or higher and the Ghostscript libraries are both required.

# Running the Software

1. Check your version of Java. At least Java 8 (v1.8) is recommended:
    ```
    $ java -version
    ```
1. Make sure you're in the directory noted in step 7 above (the following command assumes you cloned the repository into your home directory):
    ```
    $ cd ~/bitcurator-redact-pdf/build/libs
    ```
1. Run Java, using the `-jar` option:
    ```
    $ java -jar bitcurator-redact-pdf-all-1.0.jar
    ```
1. The natural language parser (Stanford's CoreNLP) tends to use up a lot of memory. You may want to increase the amount of memory available to Java or to your VM to make the program run more efficiently. For instance, you can use the `-mx` option to change the maximum memory allocation for Java. The following java command will allow the program to use up to 3GB of memory:
    ```
    $ java -jar -mx3g -jar bitcurator-redact-pdf-all-1.0.jar
    ```
1. If you open the Properties dialog for the jar file in the file browser, you can set the executable flag, found under the Permissions tab. This will allow you to run the software in the future by double-clicking on the jar file. When you open a file browser, you will be shown your home directory. Assuming you downlaoded the **bitcurator-redact-pdf** repository into your home directory, you should see a **bitcurator-redact-pdf** folder. Double-click on it, then double click on **build**, then double-click on **libs**. Right-click on the jar file, select **Properties** from the context menu, and then click the **Permissions** tab. Check the box labeled **"Allow executing file as a program."**

# Quick Start Guide

## The Main Window

When the BitCurator Redactor program starts, you see a large main windows divided into left and right regions. The left region is a table of your PDF files, which is empty at the start. The right region has two tabs, one for Named Entities and another for other Text Patterns. The table of named entities is empty at the start, while the Text Patterns will show a set of default regular expressions.
Along the top of the window is the main menu, showing **PDF Files**, **Entity Recognition**, and **Text Patterns**. These menus let you manipulate the various tables shown in the main window.

## How to Redact a File

To begin working with a PDF file, first you will *Open* the file from the PDF Files menu. After choosing a PDF file, you will see it listed on the left table. Named entity recognition runs immediately after a PDF is opened, so after a few seconds you may see some items appear in the Named Entities table as well.

NOTE: If your PDF file is highlighted red in the table row, this means that the software cannot read your PDF properly. Due to the frequency of faulty PDF generating software this happens every so often. In such cases you cannot redact that PDF with the software.

To begin redaction, simply double-click on the PDF file in the table on the left. This will open the redaction preview dialog, which shows you a list of each individual entity or patterns discovered in the document. A page preview is shown on the left with boxes around the relevant text areas. By clicking on the entities or patterns on the right, you can move the preview to show the relevant context of the entity or pattern on the page. Make your redaction choices using the drop-down menu in the table on the right, choosing options of **Redact** or **Ignore**. The **Ask** action, if showing, must be replaced by either **Ignore** or **Redact**. Preview text boxes are color-coded, with **Redact** areas shown in gray and **Ask** areas highlighted in yellow. Once your selections have been made, you can redact the file by clicking on the **Redact** button. Otherwise, you can use the **Cancel** button to close the dialog.

The redacted PDF file will be saved into an "output" folder under the current working directory, i.e. the directory from which you ran the software. The entire path to the redacted file is duplicated within this folder, which lets one redact many files without worrying about overwriting output files.

## Setting Default Actions

Sometimes you want to apply a default action to every instance of an entity or pattern. In that case you can set the default action in the table on the main window. By selecting **Ignore**, **Redact**, or **Ask**, you ensure that these actions are pre-selected for all PDFs in the Redaction Preview dialog. **Ask** means "ask me about each instance" of the given entity or pattern, while showing you the document context in the page preview. You must resolve all **Ask** actions in the Redaction Preview dialog before you can redact the file.

## Text Patterns

The text patterns table shows a list of regular expressions that will be detected in your PDF text. The initial default pattern shows a social security number example. Patterns are given a label, an expression, and a default action. You can save or load patterns from tab-delimited text files. To add a new pattern, use the Text Patterns menu. Right-click to delete an existing pattern. You can edit pattern labels and expressions by clicking in the relevant portion of the table.

### Import Bulk Extractor Features

You have the option of loading one or more feature files from Bulk Extractor as text patterns. Using the "Import Bulk Extractor features.." menu item and then choose your feature files in the file dialog. Each feature is added to the Text Patterns table, with a regular expression that is a literal quote of the feature text. This will find an exact match for the feature text.

## Sample File Walk-Through
We are going to proceed step-by-step through the redaction of a sample file, `samples/Abstract.pdf` in your bitcurator-redact-pdf project folder. This is an abstract for an unrelated grant project. It mentions several named entities and is a properly formatted PDF file. Let's get started.

1. First open the *BitCurator Redactor* program. As a reminder, this file is located at **build/libs/bitcurator/bitcurator-redact-pdf-all-1.0.jar** inside the **bitcurator-redact-pdf** folder. If you set the executable permissions earlier, you can double-click on this file, or from a terminal (assuming you cloned the repository into your home directory) run the following:
   ```
   $ cd ~/bitcurator-redact-pdf/build/libs
   $ java -jar bitcurator-redact-pdf-all-1.0.jar
   ```

1. Go to the **PDF Files** menu and select **Open File(s)..**.

1. Now choose the `Abstract.pdf` file in the `bitcurator-redact-pdf/samples` folder.

1. The file is added to the PDF table on the left-hand side of the window. Named entity recognition is in progress and in a few moments you will see a list of persons, locations, and organizations on the right-hand side of the window.

1. Notice that "Cassandra" is listed as a person. While Cassandra is a person's name, in this document it is the name of a software program. We don't need to redact "Cassandra" so we will leave the default action of **Ignore** for it.

1. "NCSA" is the acronym for the National Center for Supercomputing Applications. Let's redact this entity everywhere it occurs in our document. Select **Redact** from the drop-down menu by it.

1. "Archive Analytics" was the name of a software company. We might want to redact this or not, depending upon the context in which it is used. Let's have the program ask us about each occurrence of this entity. Select **Ask** from the drop-down menu by it.

1. Now switch to the **Text Patterns** tab. If this is a newly installed program, you will have only the Social Security Number pattern in this table. There are not any social security numbers in this document. However, there is a software name that was not detected by our NLP processing. "DRAS-TIC" is the name of a software project that may sometimes include the hyphen and sometimes not. Let's create a pattern to redact "DRASTIC" or "DRAS-TIC".

1. Using the **Text Patterns** menu, select **New Pattern**.

1. Edit the label to say "DRASTIC".

1. Edit the expression to read "DRASTIC|DRAS.TIC". This will detect either the hyphenated or non-hyphenated form. It will also detect cases where the hyphen is instead written as a block or dot, following dictionary-style syllable indicators.

1. Edit the default action to read **Ask**.

1. Now we are going to preview our redaction choices. Do this by double-clicking on the name of our file in the left-hand side table. The Redaction Preview dialog opens.

1. This is a one-page PDF. The entities and patterns are highlighted in color-coded boxes on the page preview on the left-hand side. The table on the right-hand side shows a list of the entities and patterns.

1. Click on the first instance of the "DRASTIC" pattern in the table. The preview panel now outlines this entity in red. Notice that the original text is "DRAS-TIC" with a hyphen in it.

1. We want to redact this instance of the pattern, so use the drop-down menu in the table to select **Redact**. Notice that the highlight color chances to gray. The gray box is a hint that the area will be covered in a black box when redaction is completed.

1. Please make other selections until the table not longer shows **Ask** anywhere.

1. When ready, press the **Redact** button.

1. The dialog windows closes. The program has saved your redacted PDF file in the output folder.

## Acknowledgements

Greg Jansen developed the BitCurator Redact PDF under contract for the BitCurator NLP project.

## License(s)

The BitCurator logo, BitCurator project documentation, and other non-software products of the BitCurator team are subject to the the Creative Commons Attribution 4.0 Generic license (CC By 4.0).

Unless otherwise indicated, software items in this repository are distributed under the terms of the GNU Lesser General Public License, Version 3. See the text file "COPYING" for further details about the terms of this license.

In addition to software produced by the BitCurator team, BitCurator packages and modifies open source software produced by other developers. Licenses and attributions are retained here where applicable.

Tell Me First's core
====================

This repository contains the core of
[TellMeFirst](https://github.com/TellMeFirst/TellMeFirst).

TellMeFirst is a tool for classifying and enriching
textual documents via Linked Open Data.
It uses [Lucene](http://lucene.apache.org/core/) indexes
for its classification and enrichment system. To build such
indexes use our [fork of the DBpedia Spotlight
project](https://github.com/TellMeFirst/dbpedia-spotlight/tree/tellmefirst).

The core of TellMeFirst is the lowest level component that
directly interacts with Lucene.

Use the API exported by this module as follows:

    // Initialize settings and index
    TMFVariables variables = new TMFVariables("/path/to/config/file");
    IndexesUtil.init();

    // Classify
    Classifier classifier = new Classifier(language);
    List<String[]> res = classifier.classify(text, numTopics, language);

See also how the API is used by the modules using TMF's core.

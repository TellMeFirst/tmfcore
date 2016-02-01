TellMeFirst's core
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

Use the API exported by this module as follows. See also how the API is
used by [tmfcore_build_cli](https://github.com/bassosimone/tmfcore_build_cli/blob/master/tmfcore_cli/src/main/java/it/polito/tellmefirst/cli/TMFCoreCli.java) and by
[tmfcore_build_war](https://github.com/bassosimone/tmfcore_build_war/blob/master/tmfcore_jaxrs/src/main/java/it/polito/tellmefirst/jaxrs/ClassifyResource.java).

API Initialization
------------------

First you need to initialize the settings and the index using
the following code:

    TMFVariables variables = new TMFVariables("/path/to/config/file");
    IndexesUtil.init();

API Usage
---------

Once you have initialized TMF's core, as described above, you can
invoke `classify()` to classify text.

    //
    // Here `text` is a String, `numTopics` is a integer and `language`
    // is again a String (typically either "en" or "it").
    //
    Classifier classifier = new Classifier(language);
    List<String[]> res = classifier.classify(text, numTopics);

The `classify()` function follows the traditional TMF policy by which
large texts are divided in chunks classified separately, and the result
is generated merging the classification of each chunk of text.

You can bypass this policy by using the `classifyShortText()` function
that directly passes the text to Lucene. Note, however, that depending on
the Lucene configuration and on the text length, this call may raise an
exception if the resulting Lucene query is too large.

    Classifier classifier = new Classifier(language);
    List<String[]> res = classifier.classifyShortText(text, numTopics);

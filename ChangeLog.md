ChangeLog
=========

v2.0.0.0 (Tue, 25 Nov 2014)
---------------------------

* Add custom `pom.xml` file, curate the tree and reindent code.

* Expose `classifyShortText()` to allow clients to control where
  to split long texts (thus separating mechanism and policy).

* Change the way in which `IndexUtils` is initialized; now one needs
  to call the static `init()` method.

* Change the API of `classify()` to receive only the text, the number of
  topics and the classifier's language.

* Remove everything unrelated with `classify()`.

* Merge Telecom Italia changes from [thermz/tellmefirst](https://github.com/thermz/tellmefirst).

v1.0.0.0 (Fri, 19 Sep 2014)
---------------------------

* Import the unmodified code from [TellMeFirst/tellmefirst@5b5f0ae9b3](https://github.com/TellMeFirst/tellmefirst/commit/5b5f0ae9b3).

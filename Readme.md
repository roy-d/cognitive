# Analytics on unstructured text using IBM Watson
## (using Scala and Akka)  

Use `#roybot` on twitter or, Listener for speech to text.

##Watson APIs used
* Conversation
* Alchemy Language
* Tone Analyzer
* NLC
* Personality Insight
* Speech to text
* more soon !

CI: 
```sbt clean test```

Run twitter bot: 
```sbt "run-main lab.Server"```

Run speech bot: 
```sbt "run-main lab.Listener"```

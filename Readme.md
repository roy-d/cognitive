# Analytics on unstructured text using IBM Watson
## (using Scala and Akka)  

Use `#roybot` on twitter.

##Watson APIs used
* Conversation
* Alchemy Language
* Tone Analyzer
* NLC
* Personality Insight
* more soon !

CI: 
```sbt clean test```

Run: 
```sbt "run-main lab.Server"```

Tweets that produce interesting analytics results:
```
#roybot analyze: when Cristiano Ronaldo attended the conference at Vienna he suffered from Crushing injury of left thigh, sequela feel bad
#roybot analyze: Even with an Impingement syndrome of unspecified shoulder Bobby Flay kept competing with top iron chefs like a super hero
#roybot analyze: I am so happy to fly to Miami from New York on August 26th of 2016 so i can escape from this cough and flu to play soccer
#roybot analyze: Just got to admire Donald Trump as he is more clever than Donald Duck who will always be my favourite cartoon character
```

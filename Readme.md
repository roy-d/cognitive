# Analytics on unstructured text using IBM Watson
## (using Scala and Akka)  

CI: 
```sbt clean test```

Run: 
```sbt "run-main lab.Server"```

Tweets that produce interesting results:
```
#roybot when Cristiano Ronaldo attended the conference at Vienna he suffered from Crushing injury of left thigh, sequela i feel sorry
#roybot Even with an Impingement syndrome of unspecified shoulder Bobby Flay kept competing with the top iron chefs like a super hero
#roybot I am so happy to fly to Miami from New York on August 26th of 2016 so i can escape from all this cough and flu to play some soccer
#roybot Just got to admire Donald Trump as he is more clever than Donald Duck who will always be my favourite cartoon character
```

Sometimes Twitter4j just craps its pants:
```
TwitterException{exceptionCode=[2fc5b7cb-0ec8429f], statusCode=403, message=This request looks like it might be automated. To protect our users from spam and other malicious activity, we can't complete this action right now. Please try again later., code=226, retryAfter=-1, rateLimitStatus=null, version=4.0.5}
```

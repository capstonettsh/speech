1. load up the springboot project
2. run KafkaApplpication.java
3. open up http://localhost:8080/ and http://localhost:8080/api/chat/consumerLogs in any browser
4. let springboot terminal run; once "quickstart-0" is seen, press "start mock data stream" button
5. wait until all messages processed to refresh /consumerLogs endpoint; will then see the full grouped output

** next steps:
get top 3 emotion and attach to each exchange
pass in each exchange to GPT
attach a review
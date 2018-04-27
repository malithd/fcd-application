FROM bde2020/kafka:2.11-0.10.2.0


RUN chmod +x /wait-for-step.sh
RUN chmod +x /execute-step.sh
RUN chmod +x /finish-step.sh

COPY healthcheck /
COPY kafka-startup.sh /
COPY entrypoint.sh /

EXPOSE 9092

RUN chmod +x /entrypoint.sh
RUN chmod +x /healthcheck
RUN chmod +x /kafka-startup.sh

ENTRYPOINT ["/entrypoint.sh"]

CMD [ "./kafka-startup.sh" ]

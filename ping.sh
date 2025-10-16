#!/usr/bin/env bash
curl --connect-timeout 1 -s http://localhost:8080/notification-command-api/internal/metrics/ping 
curl --connect-timeout 1 -s http://localhost:8080/notification-command-controller/internal/metrics/ping 
curl --connect-timeout 1 -s http://localhost:8080/notification-command-handler/internal/metrics/ping 
curl --connect-timeout 1 -s http://localhost:8080/notification-event-listener/internal/metrics/ping 
curl --connect-timeout 1 -s http://localhost:8080/notification-event-processor/internal/metrics/ping 
curl --connect-timeout 1 -s http://localhost:8080/notification-query-api/internal/metrics/ping 
curl --connect-timeout 1 -s http://localhost:8080/notification-query-controller/internal/metrics/ping 
curl --connect-timeout 1 -s http://localhost:8080/notification-query-view/internal/metrics/ping

# APNS mock

Use netem to alter network bandwith, see https://wiki.linuxfoundation.org/networking/netem.

- show actual queues: `sudo tc qdisc show dev lo`
- add network delay: `sudo tc qdisc change dev lo root netem delay 100ms 10ms 25%`
- add network delay and rate limiting: `sudo tc qdisc replace dev lo root netem delay ${DELAY_MS}ms rate ${RATE_MBIT}Mbit limit ${LIMIT_PKTS}`

# NewRelic Integration Setup Instructions

This document provides instructions for setting up NewRelic logs and infrastructure monitoring integration.

## Prerequisites
- Linux/Unix-based system with bash shell
- sudo privileges
- Internet connectivity

## Installation Command

```bash
curl -Ls https://download.newrelic.com/install/newrelic-cli/scripts/install.sh | bash && \
sudo NEW_RELIC_API_KEY=NRAK-0EB1MNEE7PMHZQ93UAXYJTFNEQT \
NEW_RELIC_ACCOUNT_ID=6349917 \
/usr/local/bin/newrelic install -n logs-integration -y
```

## What This Does

1. Downloads and installs the NewRelic CLI tool
2. Configures the integration with your specific:
   - API Key: `NRAK-0EB1MNEE7PMHZQ93UAXYJTFNEQT`
   - Account ID: `6349917`
3. Installs the logs integration package
4. Automatically enables infrastructure and logs metrics monitoring

## Verification

After running the command:
1. Check the NewRelic dashboard to verify data is being received
2. Look for the new host in your infrastructure list
3. Verify logs are being collected and displayed in the Logs section

## Support

If you encounter any issues, please:
1. Verify your internet connection
2. Ensure you have proper sudo privileges
3. Check the NewRelic logs in `/var/log/newrelic`
4. Contact NewRelic support if problems persist

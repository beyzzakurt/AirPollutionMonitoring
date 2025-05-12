#!/bin/bash

source "$(dirname "$0")/config.sh"

DURATION=60
RATE=1
ANOMALY_CHANCE=10

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --duration=*) DURATION="${1#*=}" ;;
        --rate=*) RATE="${1#*=}" ;;
        --anomaly-chance=*) ANOMALY_CHANCE="${1#*=}" ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
    shift
done

END_TIME=$((SECONDS + DURATION))

while [ $SECONDS -lt $END_TIME ]; do
    for ((i = 0; i < RATE; i++)); do
        LAT=$(awk -v min=35 -v max=42 'BEGIN{srand(); print min+rand()*(max-min)}')
        LON=$(awk -v min=25 -v max=45 'BEGIN{srand(); print min+rand()*(max-min)}')

        function gen_value() {
            local base_min=$1
            local base_max=$2
            local anomaly_min=$3
            local anomaly_max=$4
            if (( RANDOM % 100 < ANOMALY_CHANCE )); then
                awk -v min=$anomaly_min -v max=$anomaly_max 'BEGIN{srand(); print min+rand()*(max-min)}'
            else
                awk -v min=$base_min -v max=$base_max 'BEGIN{srand(); print min+rand()*(max-min)}'
            fi
        }

        PM25=$(gen_value 10 30 300 400)
        PM10=$(gen_value 20 50 200 300)
        NO2=$(gen_value 10 30 150 200)
        SO2=$(gen_value 5 15 100 150)
        O3=$(gen_value 20 60 180 250)

        TIMESTAMP=$(date +%s%N)

        # InfluxDB line protocol
        LINE="air_quality,lat=$LAT,lon=$LON PM25=$PM25,PM10=$PM10,NO2=$NO2,SO2=$SO2,O3=$O3 $TIMESTAMP"

        # Send to InfluxDB
        curl -s -X POST "$INFLUX_URL/api/v2/write?org=$ORG&bucket=$BUCKET&precision=ns" \
            --header "Authorization: Token $TOKEN" \
            --data-binary "$LINE"

        # Print to terminal
        echo "Sent: lat=$LAT lon=$LON PM25=$PM25 PM10=$PM10 NO2=$NO2 SO2=$SO2 O3=$O3"
    done
    sleep 1
done

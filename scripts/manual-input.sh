#!/bin/bash
source "$(dirname "$0")/config.sh"

if [ "$#" -ne 7 ]; then
    echo "Usage: $0 <latitude> <longitude> <PM25> <PM10> <NO2> <SO2> <O3>"
    exit 1
fi

LAT=$1
LON=$2
PM25=$3
PM10=$4
NO2=$5
SO2=$6
O3=$7


TIMESTAMP=$(date +%s%3N)
echo "Using timestamp: ${TIMESTAMP}"


curl -XPOST "http://localhost:8086/api/v2/write?bucket=AirPollutionData&org=AirPollution&precision=ms" \
  -H "Authorization: Token ye6JWmAsopJdcom-khY0tB5ichosO6nqOWpaeeAz9JelZfX078y3bB5aqtkXWVcsS3rOqvTO2y5JqEUVjOJ5_Q==" \
  -H "Content-Type: text/plain; charset=utf-8" \
  --data-binary "air_quality,lat=${LAT},lon=${LON} PM25=${PM25},PM10=${PM10},NO2=${NO2},SO2=${SO2},O3=${O3} ${TIMESTAMP}"


echo "Response Code: ${RESPONSE}"


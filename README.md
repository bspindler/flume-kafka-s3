# Analytics Flume S3
Pull `analysis.results` from Kafka, extract `MetricResult` objects, and push them to S3

## Building
To build **Analytics Flume S3** into the thinnest image run `docker build -t netuitive/analytics-flume-s3`.

To build within a dev environment run `docker build -f Dockerfile-dev -t netuitive/analytics-flume-s3`

## Environment Variables
- **ZOOKEEPER_URL** - Zookeeper URL for communicating with Kafka
- **AWS_ACCESS_KEY_ID** - AWS access key
- **AWS_SECRET_ACCESS_KEY** - AWS secret key
- **AWS_BUCKET** - AWS bucket for CSV storage
- **AWS_BUCKET_FORMAT** (default: /%Y/%m/%d/%H) - S3 folder prefix format
- **FILE_PREFIX** (default: flume-s3) - S3 file prefix

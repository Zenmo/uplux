source credentials.env
podman run -it quay.io/minio/minio:latest bash
mc alias set myminio https://minio.lux.energy $UPLUX_ACCESS_KEY $UPLUX_SECRET_KEY
mc ilm rule add --expire-days 1 myminio/temporary-downloads
mc ilm rule ls myminio/temporary-downloads

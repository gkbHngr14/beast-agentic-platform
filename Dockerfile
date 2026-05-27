FROM ubuntu:latest
LABEL authors="nagen"

ENTRYPOINT ["top", "-b"]
FROM golang:1.23 as builder

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN go build -o main .

FROM ubuntu:latest as runner

RUN apt-get update && apt-get install -y \
    clang-tidy && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/main /app/main

WORKDIR /app

EXPOSE 8082

CMD ["./main"]

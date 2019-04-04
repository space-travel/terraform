# terraform

> Have you ever considered the possibility of Mars meeting us half-way?
>
> \- Arthur C. Clarke in _The Sands of Mars_

## Build

Assuming Clojure is available in your `PATH` you can generate an Uberjar named `terraform.jar` via...

```bash
./build.sh
```

## Usage

To generate a directory structure for a podcast RSS feed (located at `https://example.com/rss/podcast.rss`) suitable for ingestion into `station` or jettison by `escape-pod` we would run...

```bash
java -jar terraform "https://example.com/rss/podcast.rss"
```

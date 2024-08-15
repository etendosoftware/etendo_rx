.PHONY: build

VERSION = 1.0.0

build:
	echo "Build amd64"
	docker buildx build --platform=linux/amd64 -t etendo/dynamic-das:$(VERSION)-amd64 .
	docker push etendo/dynamic-das:$(VERSION)-amd64
	echo "Build arm64"
	docker buildx build --platform=linux/arm64 -t etendo/dynamic-das:$(VERSION)-arm64 .
	docker push etendo/dynamic-das:$(VERSION)-arm64
	echo "Create manifest"
	docker pull etendo/dynamic-das:$(VERSION)-amd64
	docker pull etendo/dynamic-das:$(VERSION)-arm64
	docker manifest create etendo/dynamic-das:$(VERSION) etendo/dynamic-das:$(VERSION)-amd64 etendo/dynamic-das:$(VERSION)-arm64
	docker manifest annotate etendo/dynamic-das:$(VERSION) etendo/dynamic-das:$(VERSION)-amd64 --os linux --arch amd64
	docker manifest annotate etendo/dynamic-das:$(VERSION) etendo/dynamic-das:$(VERSION)-arm64 --os linux --arch arm64
	docker manifest push etendo/dynamic-das:$(VERSION)


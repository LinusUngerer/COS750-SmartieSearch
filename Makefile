.PHONY: test build clean docs

test:
	mvn -B test

build:
	mvn -B package

clean:
	mvn -B clean

docs:
	cd docs/SE_Document && latexmk -pdf main.tex
	cd docs/ID_Document && latexmk -pdf main.tex

.PHONY: test build run clean docs

test:
	mvn -B test

build:
	mvn -B package

run:
	mvn -B javafx:run

clean:
	mvn -B clean

docs:
	cd docs/SE_Document && latexmk -pdf main.tex
	cd docs/ID_Document && latexmk -pdf main.tex

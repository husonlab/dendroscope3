manual.pdf: manual.tex clean
	pdflatex manual
	bibtex manual
	pdflatex manual
	pdflatex manual
	makeindex manual
	pdflatex manual

clean:
	rm -f manual.dvi manu.idx manual.aux manual.ps manual.pdf

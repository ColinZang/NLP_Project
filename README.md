# NLP_Project

In this project we worked on three aspects of Chinese language processing: Chinese word segmentation, Part-of-Speech tagging and parsing. For word segmentation, we built Conditional Random Fields model with lexicon-based, morphological and dictionary-derived features. For POS tagging we built a trigram HMM tagger with Viterbi algorithm. For parsing we used the method of probabilistic CKY parsing of PCFGs.  

Inside the 'src' directiory, there are three major packages: 'ChineseSegmentation', 'HMMPOSTagger' and 'ChineseParse', corresponding to the three steps in Chinese language processing.

The other three packages are helpers (adjusting training data format, scoring etc.).

This project consists of around 3500 lines of codes, and was primarily written in Java.

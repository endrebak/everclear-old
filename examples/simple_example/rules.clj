(defrule bwa-map
  "Map DNA sequences against a reference genome with BWA."
  {:wildcards [:sample :genome]
   :external [:genome :fastq]
   :output {:bam "bwa-map.bam" :bam2 "bwa-map2.bam"}
   :threads 8
   :params {:rg "@RG\tID:{sample}\tSM:{sample}"}
   :shell "bwa mem -R '{params.rg}' {threads} {file.genome} {file.fastq} | samtools view -Sb - > {out}"})

(defrule samtools-sort
  "Sort the bams."
  {:wildcards [:sample :genome]
   :input "bwa-map.bam"
   :output ["bam/sorted.bam"]
   :shell "samtools sort -T {wildcards.sample} -O bam {in} > {out}"})

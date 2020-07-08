import pandas as pd
import csv

interventionMapXls = pd.read_excel(open("InterventionTaxonomyMap.xlsx", "rb"), sheet_name = "Sheet1")

with open('InterventionTaxonomyMapEvaluation.tsv', 'wt') as out_file:
    tsv_writer = csv.writer(out_file, delimiter='\t')

    for i in range(245):
        intervention = interventionMapXls.iloc[i]["intervention_type_name"]
        intervention_map_raw = str(interventionMapXls.iloc[i]["map_to_WM_ontology"])

        print("-"*20)
        print(i)
        print(intervention)
        print(intervention_map_raw)

        if len(intervention_map_raw)>1:  # filter out unsure ones
            if intervention_map_raw[-1]=="?":
                exact_match = 0
                intervention_map = intervention_map_raw[:-2]
            else:
                exact_match = 1
                intervention_map = intervention_map_raw

            tsv_writer.writerow([intervention, intervention_map, str(exact_match)])


        

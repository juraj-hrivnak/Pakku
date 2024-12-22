import itertools
from pathlib import Path


def generate_combinations_list():
    cases = ['Game Added',
             'Game Removed',
             'Loader Added',
             'Loader Removed',
             'Loader Updated',
             'Project Added',
             'Project Removed',
             'Project Updated']

    # Generate all combinations of the cases
    all_combinations = []

    # Create combinations for every possible length (from 1 to len(cases))
    for r in range(1, len(cases) + 1):
        all_combinations.extend(itertools.combinations(cases, r))

    output_path = Path('output')
    combinations_file = Path(output_path / 'combinations.txt')
    with combinations_file.open('w') as f:
        for combo in all_combinations:
            f.write(', '.join(combo) + '\n')


if __name__ == '__main__':
    generate_combinations_list()

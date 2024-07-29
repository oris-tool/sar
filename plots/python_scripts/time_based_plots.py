import gzip
import matplotlib.pyplot as plt
from matplotlib.ticker import (AutoMinorLocator, MultipleLocator)
from collections import defaultdict

plt.rcParams['text.usetex'] = True
plt.rcParams['axes.grid'] = True
plt.rcParams['grid.linestyle'] = 'dotted'


def best_period(pdf, periods, unavailability,
                undetected_failure, useless, average_reward):
    _, ax = plt.subplots(figsize=(4, 3))
    ax.set_xlim(0, 4320)
    ax.set_ylim(0, 0.012)
    ax.xaxis.set_major_locator(MultipleLocator(2000))
    ax.yaxis.set_major_locator(MultipleLocator(0.001))
    colors = plt.rcParams['axes.prop_cycle'].by_key()['color']
    plt.plot(periods, unavailability, '-', linewidth=0.5, marker='o', markersize=1, label=r'P(\emph{system unavailable})', color=colors[0])
    plt.plot(periods, undetected_failure, '-', linewidth=0.5, marker='o', markersize=1, label=r'P(\emph{undetected failure})', color=colors[1])
    plt.plot(periods, average_reward, '-', linewidth=0.5, marker='o', markersize=1, label=r'[P(\emph{system unavailable}) + P(\emph{undetected failure})]/2', color=colors[2])
    best_metric = min(average_reward)
    best_period = periods[average_reward.index(best_metric)]
    plt.scatter([best_period], [best_metric], color='red', s=5, zorder=2)
    plt.axvline(best_period, color='red', linewidth=0.5, zorder=2)
    plt.legend(fontsize=7, loc='upper right')
    plt.ylabel('probability')
    plt.xlabel(r'rejuvenation period $p$ (minutes)')
    plt.savefig(pdf, bbox_inches='tight')

def unreliability(pdf, time, unreliability):
    _, ax = plt.subplots(figsize=(4, 3))
    ax.set_xlim(0, 4320)
    ax.set_ylim(0, 1.1)
    ax.xaxis.set_major_locator(MultipleLocator(2000))
    ax.yaxis.set_major_locator(MultipleLocator(0.1))
    colors = plt.rcParams['axes.prop_cycle'].by_key()['color']
    if len(unreliability) == 1:
        plt.plot(time, next(iter(unreliability.values())), '-', linewidth=1, color=colors[3])
    else:
        colors = ['red', 'blue', 'magenta', 'green']
        for i, (label, ys) in enumerate(unreliability.items()):
            plt.plot(time, ys, '-', linewidth=1, label=label, color=colors[i])
        plt.legend(fontsize=7, loc='upper left')
        if len(unreliability.items()) == 3:
            ax.set_xlim(0, 400000)
            ax.xaxis.set_major_locator(MultipleLocator(100000))
    plt.ylabel(r'unreliability')
    plt.xlabel(r'time (min)')
    plt.savefig(pdf, bbox_inches='tight')

def cumulative_unavailability(pdf, time, cumulative_unavailability, *, xmin=None, xmax=None, ymax=None, xstep=None, loclegend=None):
    _, ax = plt.subplots(figsize=(4, 3))
    colors = plt.rcParams['axes.prop_cycle'].by_key()['color']
    if len(cumulative_unavailability) == 1:
        ax.set_xlim(0 if xmin is None else xmin, 4320 if xmax is None else xmax)
        ax.set_ylim(0, 35.0 if ymax is None else ymax)
        ax.xaxis.set_major_locator(MultipleLocator(2000 if xstep is None else xstep))
        ax.yaxis.set_major_locator(MultipleLocator(5.))
        plt.plot(time, next(iter(cumulative_unavailability.values())), '-', linewidth=1, color=colors[3])
    else:
        ax.set_xlim(0 if xmin is None else xmin, 4320 if xmax is None else xmax)
        ax.set_ylim(0, 35.6 if ymax is None else ymax)
        ax.xaxis.set_major_locator(MultipleLocator(2000 if xstep is None else xstep))
        ax.yaxis.set_major_locator(MultipleLocator(5.))

        colors = ['red', 'blue', 'magenta', 'green']
        for i, (label, ys) in enumerate(cumulative_unavailability.items()):
            plt.plot(time, ys, '-', linewidth=1, label=label, color=colors[i])
        plt.legend(fontsize=7, loc=('upper left' if loclegend is None else loclegend))
    plt.ylabel(r'cumulative unavailability (min)')
    plt.xlabel(r'time (min)')
    plt.savefig(pdf, bbox_inches='tight')

def instant_unavailability(pdf, time, instant_unavailability, *, xmin=None, xmax=None, xstep=None, loclegend=None):
    _, ax = plt.subplots(figsize=(4, 3))
    ax.set_xlim(0 if xmin is None else xmin, 4320 if xmax is None else xmax)
    ax.set_ylim(0., 0.9)
    ax.xaxis.set_major_locator(MultipleLocator(2000 if xstep is None else xstep))
    ax.yaxis.set_major_locator(MultipleLocator(0.2))
    colors = plt.rcParams['axes.prop_cycle'].by_key()['color']
    if len(instant_unavailability) == 1:
        plt.plot(time, next(iter(instant_unavailability.values())), '-', linewidth=1, color=colors[3])
    else:
        colors = ['red', 'blue', 'magenta', 'green']
        for i, (label, ys) in enumerate(instant_unavailability.items()):
            plt.plot(time, ys, '-', linewidth=1, label=label, color=colors[i])
        plt.legend(fontsize=7, loc=('upper left' if loclegend is None else loclegend))
    plt.ylabel(r'Unavailability')
    plt.xlabel(r'Time (Minutes)')
    plt.savefig(pdf, bbox_inches='tight')

def read_data(csv, skip_cols=None):
    opener = gzip.open if csv.endswith('.gz') else open
    with opener(csv, 'rt') as f:
        data = f.read()
    # skip header and last empty line
    series = defaultdict(list)
    for row in data.split('\n')[1:-1]:
        for i, col in enumerate(row.split(',')):
            if not skip_cols or i not in skip_cols:
                series[i].append(float(col))
    return list(series[i] for i in sorted(series))

#CHOSE YOUR PATH
PATH = "/Users/riccardoreali/Desktop/STLAB/sar/plots/"

# BEST PERIOD ---
# Fig4: steady_state_variation_gen.pdf
data = read_data(PATH + f'data/timebased/bestPeriodGEN.csv')
best_period(PATH + f'figures/timebased/steady_state_variation_GEN.pdf', *data)
# Fig4: steady_state_variation_exp.pdf
data = read_data(PATH + f'data/timebased/bestPeriodEXP.csv')
best_period(PATH + f'figures/timebased/steady_state_variation_EXP.pdf', *data)

# UNRELIABILITY
# Fig5a: unreliability_comparison_600.pdf
data_gen600 = read_data(PATH + f'data/timebased/instantUnreliability_GEN1050_.1_4320.csv', skip_cols=[0])
data_exp600 = read_data(PATH + f'data/timebased/instantUnreliability_EXP1050_.1_4320.csv', skip_cols=[0])
assert data_gen600[0] == data_exp600[0]
unreliability(PATH + f'figures/timebased/unreliability_comparison_1050_.1.pdf', data_gen600[0],
              {'time-based rejuvenation - bounded regeneration': data_gen600[1],
               'time-based rejuvenation - enabling restriction': data_exp600[1]})

# Fig5b: unreliability_comparison_420.pdf
data_gen420 = read_data(PATH + f'data/timebased/instantUnreliability_GEN510_.1_4320.csv', skip_cols=[0])
data_exp420 = read_data(PATH + f'data/timebased/instantUnreliability_EXP510_.1_4320.csv', skip_cols=[0])
assert data_gen420[0] == data_exp420[0]
unreliability(PATH + f'figures/timebased/unreliability_comparison_500_.1.pdf', data_gen420[0],
              {'time-based rejuvenation -  bounded regeneration': data_gen420[1],
               'time-based rejuvenation - enabling restriction': data_exp420[1]})


# CUMULATIVE UNAVAILABILITY
# Fig5c: cumulative_unavailability_comparison_600.pdf
data_gen600 = read_data(PATH + f'data/timebased/cumulativeUnavailability_GEN1050_.1_4320.csv', skip_cols=[0])
data_exp600 = read_data(PATH + f'data/timebased/cumulativeUnavailability_EXP1050_.1_4320.csv', skip_cols=[0])
assert data_gen600[0] == data_exp600[0]
cumulative_unavailability(PATH + f'figures/timebased/cumulative_unavailability_comparison_1050_.1.pdf', data_gen600[0],
                          {'time-based rejuvenation - bounded regeneration': data_gen600[1],
                           'time-based rejuvenation - enabling restriction': data_exp600[1]})

# Fig5d: cumulative_unavailability_comparison_420.pdf
data_gen420 = read_data(PATH + f'data/timebased/cumulativeUnavailability_GEN510_.1_4320.csv', skip_cols=[0])
data_exp420 = read_data(PATH + f'data/timebased/cumulativeUnavailability_EXP510_.1_4320.csv', skip_cols=[0])
data_gen420[0] = data_gen420[0][:43201]
data_gen420[1] = data_gen420[1][:43201]
assert data_gen420[0] == data_exp420[0]
cumulative_unavailability(PATH + f'figures/timebased/cumulative_unavailability_comparison_420_.1.pdf', data_gen420[0],
                          {'time-based rejuvenation - bounded regeneration': data_gen420[1],
                           'time-based rejuvenation - enabling restriction': data_exp420[1]})

package com.omneria.fordix.client.gui.vanilla;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The clicks that tidy a run of slots: first merge identical stacks (filling the earliest ones), then order what is left
 * and pack it at the start. Works on a plain description of the slots so it can be tested without the game; the caller
 * turns each move into inventory clicks. Pure Java, unit tested.
 */
public final class SortPlan {
    /** A slot's content: what makes two stacks stackable together, how many, the stack limit and where it sorts. */
    public record Entry(String same, int count, int max, String order) {
        Entry withCount(int count) {
            return new Entry(same, count, max, order);
        }
    }

    /** {@code merge}: put everything of {@code from} onto {@code to}; otherwise swap the two slots' contents. */
    public record Move(boolean merge, int from, int to) {
    }

    private SortPlan() {
    }

    /** Null entries are empty slots. */
    public static List<Move> plan(List<Entry> slots) {
        List<Move> moves = new ArrayList<>();
        Entry[] now = slots.toArray(new Entry[0]);
        int size = now.length;
        // 1. merge: the later stacks pour into the earliest ones of their kind that have room
        for (int i = size - 1; i >= 0; i--) {
            while (now[i] != null && now[i].max() > 1) {
                int target = -1;
                for (int j = 0; j < i; j++) {
                    if (now[j] != null && now[j].same().equals(now[i].same()) && now[j].count() < now[j].max()) {
                        target = j;
                        break;
                    }
                }
                if (target < 0) {
                    break;
                }
                int moved = Math.min(now[target].max() - now[target].count(), now[i].count());
                now[target] = now[target].withCount(now[target].count() + moved);
                now[i] = moved == now[i].count() ? null : now[i].withCount(now[i].count() - moved);
                moves.add(new Move(true, i, target));
            }
        }
        // 2. order the stacks and pack them at the start
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (now[i] != null) {
                order.add(i);
            }
        }
        Entry[] snapshot = now.clone();
        order.sort(Comparator.comparing((Integer i) -> snapshot[i].order()).thenComparing(i -> snapshot[i].same())
                .thenComparing(i -> -snapshot[i].count()).thenComparing(i -> i));
        int[] token = new int[size];
        for (int i = 0; i < size; i++) {
            token[i] = now[i] == null ? -1 : i;
        }
        for (int target = 0; target < order.size(); target++) {
            int wanted = order.get(target);
            if (token[target] == wanted) {
                continue;
            }
            int at = -1;
            for (int j = target + 1; j < size; j++) {
                if (token[j] == wanted) {
                    at = j;
                    break;
                }
            }
            moves.add(new Move(false, target, at));
            int swap = token[target];
            token[target] = token[at];
            token[at] = swap;
        }
        return moves;
    }

    /** What the slots hold after the moves, for tests and previews. */
    public static List<Entry> apply(List<Entry> slots, List<Move> moves) {
        List<Entry> now = new ArrayList<>(slots);
        for (Move move : moves) {
            if (move.merge()) {
                Entry from = now.get(move.from());
                Entry to = now.get(move.to());
                int moved = Math.min(to.max() - to.count(), from.count());
                now.set(move.to(), to.withCount(to.count() + moved));
                now.set(move.from(), moved == from.count() ? null : from.withCount(from.count() - moved));
            } else {
                Entry swap = now.get(move.from());
                now.set(move.from(), now.get(move.to()));
                now.set(move.to(), swap);
            }
        }
        return now;
    }
}

# Investigation Report: Rewards Business Logic

## Current State Analysis
- **Daily Check-in**: Currently functional but uses a simplified streak logic that doesn't strictly prevent same-day duplicate attempts (though UI disables the button).
- **Missions**: `RewardsViewModel.onMissionClick` immediately increments progress. `RewardsRepositoryImpl.updateMissionProgress` automatically calls `addTransaction` when the threshold is reached. This bypasses the required "Claim" step.
- **Quick Rewards**: `RewardsViewModel.onQuickRewardClick` immediately awards coins upon clicking the card (e.g., "Watch Ad" adds 50 coins instantly without an ad callback or claim step).
- **Transactions**: Created automatically by the repository during the increment step, rather than by a deliberate user "Claim" action.

## Bugs Found
1. **Instant Gratification**: Clicking "Watch Ads" or "Lucky Spin" gives coins without any task completion.
2. **Missing Claim Step**: Missions auto-award coins the moment progress hits 100%.
3. **No "Claimed" State**: Missions only have `isCompleted`. Once completed, they are effectively finished, but there's no persistent record that the reward was *taken*.
4. **Mocked Progress**: Progress is only updated via manual clicks on the mission cards themselves, not by actual app actions (Likes, Shares, etc.).

## Proposed Fixes
1. **Explicit Claiming**: Add `isClaimed` to the `Mission` and `QuickReward` models.
2. **State Separation**:
   - `IN_PROGRESS`: Show "Go" button.
   - `COMPLETED`: Show "Claim" button.
   - `CLAIMED`: Show "Claimed" label (disabled).
3. **Event-Driven Progress**: Connect `LikeViewModel` and `FeedViewModel` to the `RewardsRepository` to track real user actions.
4. **Atomic Transactions**: Ensure `addTransaction` is only called within `claimMissionReward` or `claimQuickReward` methods.

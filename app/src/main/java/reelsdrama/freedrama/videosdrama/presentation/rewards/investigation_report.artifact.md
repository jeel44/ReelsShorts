# Investigation Report: Rewards Module

## Existing Infrastructure
- **Repository**: `FakeRewardsRepository` provides static mock data. No production interface or implementation exists.
- **Models**: Core models defined (`DailyReward`, `Mission`, etc.), but lacking `Transaction` and `Persistence` states.
- **ViewModel**: `RewardsViewModel` is tightly coupled with `FakeRewardsRepository`.
- **DataStore**: A single `DataStore<Preferences>` exists for settings.
- **UI**: High-quality M3 components are ready but lack event wiring.

## Identified Issues
- **Persistence**: Coin balance and streak are not saved.
- **Logic**: Daily check-in is purely visual.
- **Redundancy**: `FakeRewardsRepository` will become obsolete after the real implementation.
- **Architecture**: Missing `RewardsRepository` interface in the domain layer.

## Proposed Strategy
1. Define `RewardTransaction` model.
2. Create `RewardsRepository` interface.
3. Implement `RewardsRepositoryImpl` using the existing `DataStore`.
4. Update `RewardsViewModel` to handle user actions (Claim, Mission completion, etc.).
5. Wire UI components to ViewModel events.

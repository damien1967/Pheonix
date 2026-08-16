package phoenix.mechanic

/**
 * One row of a GameDefinition's drop trigger table. Each DropTrigger is self-contained — its
 * condition and outcome reference only their own data, never another trigger's — so the table as
 * a whole has no ordering dependency and no trigger can be made conditional on another having
 * already fired.
 */
data class DropTrigger(val condition: DropCondition, val outcome: DropOutcome)

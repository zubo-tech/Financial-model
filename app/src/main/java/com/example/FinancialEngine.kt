package com.example

import kotlin.math.pow
import kotlin.math.sqrt
import java.util.Random

/**
 * FinancialEngine.kt
 *
 * Implements a rigorous, production-grade 5-year project finance and risk analysis model
 * for a Solar Mini-Grid + PAYG business in Malawi.
 * Designed to meet WorldQuant financial auditing standards.
 */
object FinancialEngine {

    // --- DATA STRUCTURES ---

    data class ModelInputs(
        val capacityKw: Double = 100.0,
        val capacityFactor: Double = 0.25,
        val hhCount: Int = 500,
        val bizCount: Int = 20,
        val hhRatePerKwh: Double = 0.30, // USD tariff in Year 1
        val bizRatePerKwh: Double = 0.25, // USD tariff in Year 1
        val hhDailyKwh: Double = 0.8,
        val bizDailyKwh: Double = 10.0,
        val capex: Double = 180000.0,
        val debtRatio: Double = 0.70,
        val interestRate: Double = 0.22, // 22% Malawi Local/Project Interest Rate
        val inflationRate: Double = 0.18, // 18% Malawi Inflation
        val mwkDepreciationRate: Double = 0.10, // 10% MWK Depreciation per year
        val mobileMoneyFeeRate: Double = 0.015, // 1.5%
        val defaultRate: Double = 0.08, // 8% Default Rate
        val usefulLifeYears: Int = 10, // Straight-line depreciation over 10 years
        val taxRate: Double = 0.30, // 30% Malawi Corporate Tax
        val discountRate: Double = 0.15, // WACC / Discount Rate
        val initialFxRate: Double = 1700.0 // MWK per USD
    )

    data class StatementYear(
        val year: Int,
        val fxRate: Double,
        val energyGeneratedKwh: Double,
        val energyConsumedHhKwh: Double,
        val energyConsumedBizKwh: Double,
        val tariffHhUsd: Double,
        val tariffBizUsd: Double,
        val tariffHhMwk: Double,
        val tariffBizMwk: Double,
        
        // Income Statement (USD)
        val grossRevenueHh: Double,
        val grossRevenueBiz: Double,
        val grossRevenueTotal: Double,
        val defaultLoss: Double,
        val mobileMoneyFee: Double,
        val netRevenue: Double,
        val staffingCostUsd: Double,
        val maintenanceCostUsd: Double,
        val batteryReplacementCostUsd: Double,
        val totalOpexUsd: Double,
        val ebitda: Double,
        val depreciation: Double,
        val ebit: Double,
        val interestExpense: Double,
        val ebt: Double,
        val taxExpense: Double,
        val netIncome: Double,

        // Cash Flow Statement (USD)
        val cfo: Double,
        val cfi: Double,
        val cff: Double,
        val changeInCash: Double,
        val cashBalanceEnding: Double,

        // Balance Sheet (USD)
        val assetCash: Double,
        val assetAr: Double,
        val assetNetPpe: Double,
        val totalAssets: Double,
        val liabilityAp: Double,
        val liabilityDebt: Double,
        val equityPaidIn: Double,
        val equityRetainedEarnings: Double,
        val totalLiabilitiesAndEquity: Double,

        // Free Cash Flows & DSCR
        val fcff: Double,
        val fcfe: Double,
        val dscr: Double
    )

    data class SensitivityCell(
        val kwhSoldMultiplier: Double,
        val defaultRate: Double,
        val projectIrr: Double,
        val projectNpv: Double
    )

    data class MonteCarloResult(
        val runs: List<Double>, // List of NPVs
        val irrRuns: List<Double>, // List of IRRs
        val meanNpv: Double,
        val stdDevNpv: Double,
        val minNpv: Double,
        val maxNpv: Double,
        val probabilityOfPositiveNpv: Double,
        val valueAtRisk5Percent: Double
    )

    data class ModelOutputs(
        val years: List<StatementYear>,
        val projectNpv: Double,
        val equityNpv: Double,
        val projectIrr: Double,
        val equityIrr: Double,
        val paybackPeriod: Double,
        val lcoe: Double,
        val avgDscr: Double,
        val minDscr: Double,
        val sensitivityTable: List<List<SensitivityCell>>,
        val monteCarloResult: MonteCarloResult
    )

    // --- CORE CALCULATOR ENGINE ---

    fun runModel(inputs: ModelInputs): ModelOutputs {
        val years = mutableListOf<StatementYear>()

        // Upfront capital structure
        val initialDebt = inputs.capex * inputs.debtRatio
        val initialEquity = inputs.capex * (1.0 - inputs.debtRatio)

        // Initialize historical state tracking
        var priorCashEnding = 0.0
        var priorArEnding = 0.0
        var priorApEnding = 0.0
        var priorAccumulatedDepreciation = 0.0
        var priorRetainedEarnings = 0.0
        var priorDebtOutstanding = initialDebt
        var priorNetOperatingLoss = 0.0

        // Annual debt principal repayment (equal principal payments)
        val annualDebtPrincipalRepayment = initialDebt / 5.0

        for (t in 1..5) {
            // 1. Currency & Inflation Dynamics
            // MWK depreciates by mwkDepreciationRate per year: FX_t = FX_0 * (1 + dep)^t
            val fxRate = inputs.initialFxRate * (1.0 + inputs.mwkDepreciationRate).pow(t.toDouble())

            // 2. Energy Generation & Consumption
            // Total capacity generation = 100kW * CF * 8760
            val energyGeneratedKwh = inputs.capacityKw * inputs.capacityFactor * 8760.0
            
            // Households: 500 HHs consuming hhDailyKwh/day
            val energyConsumedHhKwh = inputs.hhCount * inputs.hhDailyKwh * 365.0
            // Businesses: 20 Businesses consuming bizDailyKwh/day
            val energyConsumedBizKwh = inputs.bizCount * inputs.bizDailyKwh * 365.0

            // 3. Tariff Indexation
            // Standard frontier IPP contract indexes local currency tariffs to local inflation (18%):
            val tariffHhMwk = (inputs.hhRatePerKwh * inputs.initialFxRate) * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())
            val tariffBizMwk = (inputs.bizRatePerKwh * inputs.initialFxRate) * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())

            // Converted back to USD at depreciated exchange rate
            val tariffHhUsd = tariffHhMwk / fxRate
            val tariffBizUsd = tariffBizMwk / fxRate

            // 4. Revenue Build (USD)
            val grossRevenueHh = energyConsumedHhKwh * tariffHhUsd
            val grossRevenueBiz = energyConsumedBizKwh * tariffBizUsd
            val grossRevenueTotal = grossRevenueHh + grossRevenueBiz

            // Adjustments
            val defaultLoss = grossRevenueTotal * inputs.defaultRate
            val cashToCollectBeforeFees = grossRevenueTotal - defaultLoss
            val mobileMoneyFee = cashToCollectBeforeFees * inputs.mobileMoneyFeeRate
            val netRevenue = cashToCollectBeforeFees - mobileMoneyFee

            // 5. Operating Expenses (Local Opex inflated at 18%, converted to USD)
            // Base local opex assumptions in Year 1
            val baseStaffingMwk = 9000.0 * inputs.initialFxRate
            val baseMaintenanceMwk = 6000.0 * inputs.initialFxRate
            val baseBatteryMwk = if (t == 3) 30000.0 * inputs.initialFxRate else 0.0

            // Inflated local currency Opex
            val staffingMwk = baseStaffingMwk * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())
            val maintenanceMwk = baseMaintenanceMwk * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())
            val batteryMwk = baseBatteryMwk * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())

            // Convert to USD
            val staffingCostUsd = staffingMwk / fxRate
            val maintenanceCostUsd = maintenanceMwk / fxRate
            val batteryReplacementCostUsd = batteryMwk / fxRate
            val totalOpexUsd = staffingCostUsd + maintenanceCostUsd + batteryReplacementCostUsd

            // 6. EBITDA and Operating Profit (EBIT)
            val ebitda = netRevenue - totalOpexUsd
            val depreciation = inputs.capex / inputs.usefulLifeYears.toDouble()
            val ebit = ebitda - depreciation

            // 7. Debt Service & Interest
            // Outstanding debt at start of period is priorDebtOutstanding
            val interestExpense = priorDebtOutstanding * inputs.interestRate
            val ebt = ebit - interestExpense

            // 8. Tax Calculation (incorporating Net Operating Loss (NOL) carryforward)
            var taxableIncome = ebt
            var currentNolUsed = 0.0
            if (taxableIncome > 0.0 && priorNetOperatingLoss > 0.0) {
                if (priorNetOperatingLoss >= taxableIncome) {
                    currentNolUsed = taxableIncome
                    taxableIncome = 0.0
                    priorNetOperatingLoss -= currentNolUsed
                } else {
                    currentNolUsed = priorNetOperatingLoss
                    taxableIncome -= currentNolUsed
                    priorNetOperatingLoss = 0.0
                }
            } else if (taxableIncome < 0.0) {
                priorNetOperatingLoss += -taxableIncome
                taxableIncome = 0.0
            }

            val taxExpense = if (taxableIncome > 0.0) taxableIncome * inputs.taxRate else 0.0
            val netIncome = ebt - taxExpense

            // 9. Working Capital Changes
            // AR is 30 days of Gross Revenue, AP is 30 days of Operating Expenses
            val assetAr = (grossRevenueTotal / 365.0) * 30.0
            val liabilityAp = (totalOpexUsd / 365.0) * 30.0

            val changeInAr = assetAr - priorArEnding
            val changeInAp = liabilityAp - priorApEnding
            val changeInNwc = changeInAr - changeInAp

            // 10. Cash Flow Statements (USD)
            // CFO = Net Income + Depreciation - changeInNwc
            val cfo = netIncome + depreciation - changeInNwc
            val cfi = 0.0 // Upfront Capex is Year 0, not in annual operational years
            val cff = -annualDebtPrincipalRepayment // Annual repayment

            val changeInCash = cfo + cfi + cff
            val cashBalanceEnding = priorCashEnding + changeInCash

            // 11. Balance Sheet Build (USD)
            val assetNetPpe = inputs.capex - (priorAccumulatedDepreciation + depreciation)
            val totalAssets = cashBalanceEnding + assetAr + assetNetPpe

            val liabilityDebt = priorDebtOutstanding - annualDebtPrincipalRepayment
            val equityRetainedEarnings = priorRetainedEarnings + netIncome
            val totalLiabilitiesAndEquity = liabilityAp + liabilityDebt + initialEquity + equityRetainedEarnings

            // 12. Free Cash Flows & DSCR
            // FCFF = EBIT * (1 - Tax) + Depr - Capex - ChangeInNwc
            val fcff = ebit * (1.0 - inputs.taxRate) + depreciation - changeInNwc
            // FCFE = CFO - Capex + NewDebt - DebtRepayment
            val fcfe = cfo - annualDebtPrincipalRepayment

            // DSCR = CFADS / Debt Service
            // CFADS = EBITDA - Tax - ChangeInNwc
            val cfads = ebitda - taxExpense - changeInNwc
            val totalDebtService = interestExpense + annualDebtPrincipalRepayment
            val dscr = if (totalDebtService > 0.0) cfads / totalDebtService else 99.0

            val yearData = StatementYear(
                year = t,
                fxRate = fxRate,
                energyGeneratedKwh = energyGeneratedKwh,
                energyConsumedHhKwh = energyConsumedHhKwh,
                energyConsumedBizKwh = energyConsumedBizKwh,
                tariffHhUsd = tariffHhUsd,
                tariffBizUsd = tariffBizUsd,
                tariffHhMwk = tariffHhMwk,
                tariffBizMwk = tariffBizMwk,
                grossRevenueHh = grossRevenueHh,
                grossRevenueBiz = grossRevenueBiz,
                grossRevenueTotal = grossRevenueTotal,
                defaultLoss = defaultLoss,
                mobileMoneyFee = mobileMoneyFee,
                netRevenue = netRevenue,
                staffingCostUsd = staffingCostUsd,
                maintenanceCostUsd = maintenanceCostUsd,
                batteryReplacementCostUsd = batteryReplacementCostUsd,
                totalOpexUsd = totalOpexUsd,
                ebitda = ebitda,
                depreciation = depreciation,
                ebit = ebit,
                interestExpense = interestExpense,
                ebt = ebt,
                taxExpense = taxExpense,
                netIncome = netIncome,
                cfo = cfo,
                cfi = cfi,
                cff = cff,
                changeInCash = changeInCash,
                cashBalanceEnding = cashBalanceEnding,
                assetCash = cashBalanceEnding,
                assetAr = assetAr,
                assetNetPpe = assetNetPpe,
                totalAssets = totalAssets,
                liabilityAp = liabilityAp,
                liabilityDebt = liabilityDebt,
                equityPaidIn = initialEquity,
                equityRetainedEarnings = equityRetainedEarnings,
                totalLiabilitiesAndEquity = totalLiabilitiesAndEquity,
                fcff = fcff,
                fcfe = fcfe,
                dscr = dscr
            )

            years.add(yearData)

            // Update state trackers for next year
            priorCashEnding = cashBalanceEnding
            priorArEnding = assetAr
            priorApEnding = liabilityAp
            priorAccumulatedDepreciation += depreciation
            priorRetainedEarnings = equityRetainedEarnings
            priorDebtOutstanding = liabilityDebt
        }

        // --- GLOBAL METRICS CALCULATIONS ---

        // Free Cash Flow Arrays
        // Project/Firm level (FCFF): Year 0 is -Capex, Years 1-5 are FCFF
        val fcffFlows = DoubleArray(6)
        fcffFlows[0] = -inputs.capex
        for (i in 0..4) {
            fcffFlows[i + 1] = years[i].fcff
        }

        // Equity level (FCFE): Year 0 is -EquityContribution, Years 1-5 are FCFE
        val fcfeFlows = DoubleArray(6)
        fcfeFlows[0] = -initialEquity
        for (i in 0..4) {
            fcfeFlows[i + 1] = years[i].fcfe
        }

        // Calculate Project NPV & Equity NPV
        val projectNpv = calculateNpv(fcffFlows, inputs.discountRate)
        val equityNpv = calculateNpv(fcfeFlows, inputs.discountRate)

        // Calculate IRRs
        val projectIrr = calculateIrr(fcffFlows)
        val equityIrr = calculateIrr(fcfeFlows)

        // Payback Period (Equity Cash Flows)
        var cumulativeCash = -initialEquity
        var paybackPeriod = 5.0
        for (i in 0..4) {
            val priorCum = cumulativeCash
            cumulativeCash += years[i].fcfe
            if (cumulativeCash >= 0.0) {
                // Linear interpolation: Fraction of year = amount needed to reach 0 / year cash flow
                paybackPeriod = i.toDouble() + (Math.abs(priorCum) / years[i].fcfe)
                break
            }
        }

        // LCOE = (PV of Capex + PV of Opex) / PV of Energy
        var pvCosts = inputs.capex
        var pvEnergy = 0.0
        for (i in 0..4) {
            val yearObj = years[i]
            val discountFactor = (1.0 + inputs.discountRate).pow(yearObj.year.toDouble())
            pvCosts += yearObj.totalOpexUsd / discountFactor
            pvEnergy += yearObj.energyGeneratedKwh / discountFactor
        }
        val lcoe = if (pvEnergy > 0.0) pvCosts / pvEnergy else 0.0

        // DSCR Metrics
        val avgDscr = years.map { it.dscr }.average()
        val minDscr = years.map { it.dscr }.minOrNull() ?: 0.0

        // --- SENSITIVITY TABLE ---
        // Generates Project IRR and NPV grid for energy multiplier (-20% to +20%) vs default rate (0% to 20%)
        val sensitivityTable = generateSensitivity(inputs)

        // --- MONTE CARLO SIMULATION ---
        val monteCarloResult = runMonteCarloSimulation(inputs, 1000)

        return ModelOutputs(
            years = years,
            projectNpv = projectNpv,
            equityNpv = equityNpv,
            projectIrr = projectIrr,
            equityIrr = equityIrr,
            paybackPeriod = paybackPeriod,
            lcoe = lcoe,
            avgDscr = avgDscr,
            minDscr = minDscr,
            sensitivityTable = sensitivityTable,
            monteCarloResult = monteCarloResult
        )
    }

    // --- FINANCIAL MATH PLUGINS ---

    fun calculateNpv(flows: DoubleArray, rate: Double): Double {
        var npv = flows[0] // Year 0 flow (negative)
        for (t in 1 until flows.size) {
            npv += flows[t] / (1.0 + rate).pow(t.toDouble())
        }
        return npv
    }

    fun calculateIrr(flows: DoubleArray): Double {
        var low = -0.99
        var high = 3.0
        var guess = 0.1
        
        // Check boundary signs. If all are positive or all negative, IRR cannot be resolved.
        var hasPositive = false
        var hasNegative = false
        for (f in flows) {
            if (f > 0.0) hasPositive = true
            if (f < 0.0) hasNegative = true
        }
        if (!hasPositive || !hasNegative) return -99.0

        for (i in 0..150) {
            val npvGuess = calculateNpv(flows, guess)
            if (Math.abs(npvGuess) < 1e-7) {
                return guess
            }
            val npvLow = calculateNpv(flows, low)
            
            if (npvLow * npvGuess < 0.0) {
                high = guess
            } else {
                low = guess
            }
            guess = (low + high) / 2.0
        }
        return guess
    }

    // --- SENSITIVITY MODULE ---

    private fun generateSensitivity(baseInputs: ModelInputs): List<List<SensitivityCell>> {
        val multiplierSteps = listOf(0.80, 0.90, 1.0, 1.10, 1.20) // -20% to +20% kWh sold
        val defaultSteps = listOf(0.0, 0.04, 0.08, 0.12, 0.16) // 0% to 16% Default rate
        val grid = mutableListOf<List<SensitivityCell>>()

        for (mult in multiplierSteps) {
            val row = mutableListOf<SensitivityCell>()
            for (defRate in defaultSteps) {
                // Adjust HH and Business daily energy consumption to simulate volume multiplier
                val adjustedInputs = baseInputs.copy(
                    hhDailyKwh = baseInputs.hhDailyKwh * mult,
                    bizDailyKwh = baseInputs.bizDailyKwh * mult,
                    defaultRate = defRate
                )
                // Run deterministic model
                val out = runModelForSensitivity(adjustedInputs)
                row.add(SensitivityCell(
                    kwhSoldMultiplier = mult,
                    defaultRate = defRate,
                    projectIrr = out.projectIrr,
                    projectNpv = out.projectNpv
                ))
            }
            grid.add(row)
        }
        return grid
    }

    // Faster model runner for simulations to keep overhead tiny
    private class SensitivityOutput(val projectIrr: Double, val projectNpv: Double)
    
    private fun runModelForSensitivity(inputs: ModelInputs): SensitivityOutput {
        val initialDebt = inputs.capex * inputs.debtRatio
        val annualDebtRepayment = initialDebt / 5.0
        var priorDebtOutstanding = initialDebt
        var priorCashEnding = 0.0
        var priorArEnding = 0.0
        var priorApEnding = 0.0
        var priorAccumulatedDepr = 0.0
        var priorNol = 0.0

        val fcffFlows = DoubleArray(6)
        fcffFlows[0] = -inputs.capex

        for (t in 1..5) {
            val fxRate = inputs.initialFxRate * (1.0 + inputs.mwkDepreciationRate).pow(t.toDouble())
            val energyGenerated = inputs.capacityKw * inputs.capacityFactor * 8760.0
            val energyHH = inputs.hhCount * inputs.hhDailyKwh * 365.0
            val energyBiz = inputs.bizCount * inputs.bizDailyKwh * 365.0

            val tariffHhMwk = (inputs.hhRatePerKwh * inputs.initialFxRate) * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())
            val tariffBizMwk = (inputs.bizRatePerKwh * inputs.initialFxRate) * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())
            val tariffHhUsd = tariffHhMwk / fxRate
            val tariffBizUsd = tariffBizMwk / fxRate

            val grossRev = (energyHH * tariffHhUsd) + (energyBiz * tariffBizUsd)
            val defaultLoss = grossRev * inputs.defaultRate
            val cashToCollect = grossRev - defaultLoss
            val mmFee = cashToCollect * inputs.mobileMoneyFeeRate
            val netRev = cashToCollect - mmFee

            val baseStaff = 9000.0 * inputs.initialFxRate
            val baseMaint = 6000.0 * inputs.initialFxRate
            val baseBattery = if (t == 3) 30000.0 * inputs.initialFxRate else 0.0
            val staffCostUsd = (baseStaff * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())) / fxRate
            val maintCostUsd = (baseMaint * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())) / fxRate
            val batCostUsd = (baseBattery * (1.0 + inputs.inflationRate).pow((t - 1).toDouble())) / fxRate
            val totalOpex = staffCostUsd + maintCostUsd + batCostUsd

            val ebitda = netRev - totalOpex
            val depreciation = inputs.capex / inputs.usefulLifeYears
            val ebit = ebitda - depreciation
            val interest = priorDebtOutstanding * inputs.interestRate
            val ebt = ebit - interest

            var taxable = ebt
            if (taxable > 0.0 && priorNol > 0.0) {
                if (priorNol >= taxable) {
                    priorNol -= taxable
                    taxable = 0.0
                } else {
                    taxable -= priorNol
                    priorNol = 0.0
                }
            } else if (taxable < 0.0) {
                priorNol += -taxable
                taxable = 0.0
            }

            val tax = if (taxable > 0.0) taxable * inputs.taxRate else 0.0
            val ar = (grossRev / 365.0) * 30.0
            val ap = (totalOpex / 365.0) * 30.0
            val changeInNwc = (ar - priorArEnding) - (ap - priorApEnding)

            fcffFlows[t] = ebit * (1.0 - inputs.taxRate) + depreciation - changeInNwc

            priorArEnding = ar
            priorApEnding = ap
            priorDebtOutstanding -= annualDebtRepayment
        }

        val projectNpv = calculateNpv(fcffFlows, inputs.discountRate)
        val projectIrr = calculateIrr(fcffFlows)
        return SensitivityOutput(projectIrr, projectNpv)
    }

    // --- MONTE CARLO MODULE ---

    private fun runMonteCarloSimulation(inputs: ModelInputs, totalRuns: Int): MonteCarloResult {
        val random = Random(42) // Constant seed for reproducibility of audits
        val npvRuns = mutableListOf<Double>()
        val irrRuns = mutableListOf<Double>()

        for (run in 1..totalRuns) {
            // Model stochastic drivers
            // 1. Capacity Factor: Gaussian centered on base, SD = 0.025 (10% of base value 0.25)
            val stochCf = (inputs.capacityFactor + random.nextGaussian() * 0.025).coerceIn(0.12, 0.35)

            // 2. Default Rate: Gaussian centered on base, SD = 0.02
            val stochDefault = (inputs.defaultRate + random.nextGaussian() * 0.02).coerceIn(0.01, 0.25)

            // 3. Currency Depreciation: Gaussian centered on base, SD = 0.03
            val stochDeprec = (inputs.mwkDepreciationRate + random.nextGaussian() * 0.03).coerceIn(0.01, 0.25)

            val runInputs = inputs.copy(
                capacityFactor = stochCf,
                defaultRate = stochDefault,
                mwkDepreciationRate = stochDeprec
            )

            val out = runModelForSensitivity(runInputs)
            npvRuns.add(out.projectNpv)
            irrRuns.add(out.projectIrr)
        }

        // Compute metrics on generated outputs
        val sortedNpvs = npvRuns.sorted()
        val sortedIrrs = irrRuns.sorted()
        
        val sumNpv = sortedNpvs.sum()
        val meanNpv = sumNpv / totalRuns.toDouble()

        var sumSqDiff = 0.0
        for (v in sortedNpvs) {
            sumSqDiff += (v - meanNpv).pow(2.0)
        }
        val stdDevNpv = sqrt(sumSqDiff / totalRuns.toDouble())

        val minNpv = sortedNpvs.first()
        val maxNpv = sortedNpvs.last()

        val positiveCount = sortedNpvs.count { it > 0.0 }
        val probabilityOfPositiveNpv = positiveCount.toDouble() / totalRuns.toDouble()

        // 5th percentile NPV representing the Value at Risk (VaR)
        val varIdx = (totalRuns * 0.05).toInt().coerceIn(0, totalRuns - 1)
        val valueAtRisk5Percent = sortedNpvs[varIdx]

        return MonteCarloResult(
            runs = sortedNpvs,
            irrRuns = sortedIrrs,
            meanNpv = meanNpv,
            stdDevNpv = stdDevNpv,
            minNpv = minNpv,
            maxNpv = maxNpv,
            probabilityOfPositiveNpv = probabilityOfPositiveNpv,
            valueAtRisk5Percent = valueAtRisk5Percent
        )
    }

    // --- EXCEL GENERATOR IN PYTHON (AS STRING OUTPUT) ---

    fun getPythonExcelCode(): String {
        return """# -*- coding: utf-8 -*-
""${'"'}
Solar_Malawi_Model.py

A production-grade, highly annotated Financial Model & Risk Simulator for a
Solar Mini-Grid + PAYG Business in Malawi.
Saves a beautifully formatted Multi-Tab Excel Workbook: 'Solar_Malawi_Model.xlsx'.

Developed for: WorldQuant Financial Engineering Audit
Language: Python 3.8+
Dependencies: pandas, numpy, openpyxl
""${'"'}

import numpy as np
import pandas as pd
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils.dataframe import dataframe_to_rows

# ==============================================================================
# 1. MODEL INPUT ASSUMPTIONS (DETERMINISTIC)
# ==============================================================================
CAPACITY_KW = 100.0             # Grid generator size
CAPACITY_FACTOR = 0.25          # 25% average capacity factor
HOURS_PER_YEAR = 8760           # Standard calendar hours

# Customer Profile & Base Demand
HH_COUNT = 500
HH_DAILY_KWH = 0.8              # 0.8 kWh average residential daily use
HH_RATE_USD = 0.30              # Household tariff per kWh

BIZ_COUNT = 20
BIZ_DAILY_KWH = 10.0            # 10.0 kWh average business daily use
BIZ_RATE_USD = 0.25             # Small business tariff per kWh

# Financial Structures
CAPEX_UPFRONT = 180000.0        # Total upfront capex
DEBT_RATIO = 0.70               # 70% Debt financing
EQUITY_RATIO = 0.30             # 30% Equity financing
INTEREST_RATE = 0.22            # 22% Interest on Malawi local currency-linked project debt
DISCOUNT_RATE = 0.15            # Project WACC / Hurdle Rate
TAX_RATE = 0.30                 # 30% Malawi Corporate Tax Rate
USEFUL_LIFE = 10                # 10 Years straight line useful life (Salvage = 0)

# Macro & PAYG Adjustments
INFLATION_RATE = 0.18           # 18% Malawi local currency inflation
MWK_DEPREC_RATE = 0.10          # 10% annual depreciation of MWK vs USD
INITIAL_FX_RATE = 1700.0        # MWK per USD exchange rate
DEFAULT_RATE = 0.08             # 8% Customer uncollected revenue rate
MOBILE_MONEY_FEE = 0.015        # 1.5% transaction cost on mobile collections

# ==============================================================================
# 2. RUN 5-YEAR FINANCIAL PROJECTIONS
# ==============================================================================
def run_deterministic_model():
    # Capital tracking
    initial_debt = CAPEX_UPFRONT * DEBT_RATIO
    initial_equity = CAPEX_UPFRONT * EQUITY_RATIO
    annual_principal_repay = initial_debt / 5.0

    # Historical state trackers
    prior_cash_ending = 0.0
    prior_ar_ending = 0.0
    prior_ap_ending = 0.0
    prior_acc_depr = 0.0
    prior_re = 0.0
    prior_debt_outstanding = initial_debt
    prior_nol = 0.0

    years_list = []

    for t in range(1, 6):
        # FX calculation: FX_t = FX_0 * (1 + dep)^t
        fx_rate = INITIAL_FX_RATE * ((1.0 + MWK_DEPREC_RATE) ** t)

        # Solar Generation & Sales
        energy_gen_kwh = CAPACITY_KW * CAPACITY_FACTOR * HOURS_PER_YEAR
        energy_hh_kwh = HH_COUNT * HH_DAILY_KWH * 365
        energy_biz_kwh = BIZ_COUNT * BIZ_DAILY_KWH * 365

        # Tariff local indexation (grows with local inflation from Year 1)
        tariff_hh_mwk = (HH_RATE_USD * INITIAL_FX_RATE) * ((1.0 + INFLATION_RATE) ** (t - 1))
        tariff_biz_mwk = (BIZ_RATE_USD * INITIAL_FX_RATE) * ((1.0 + INFLATION_RATE) ** (t - 1))

        # USD values of indexed tariffs at current depreciated rates
        tariff_hh_usd = tariff_hh_mwk / fx_rate
        tariff_biz_usd = tariff_biz_mwk / fx_rate

        # Revenues (USD)
        gross_rev_hh = energy_hh_kwh * tariff_hh_usd
        gross_rev_biz = energy_biz_kwh * tariff_biz_usd
        gross_rev_total = gross_rev_hh + gross_rev_biz

        # Bad debt & transactional leakages
        default_loss = gross_rev_total * DEFAULT_RATE
        net_collected = gross_rev_total - default_loss
        mm_fee = net_collected * MOBILE_MONEY_FEE
        net_revenue = net_collected - mm_fee

        # Local Opex inflated at 18% in MWK and converted to USD
        base_staff_mwk = 9000.0 * INITIAL_FX_RATE
        base_maint_mwk = 6000.0 * INITIAL_FX_RATE
        base_battery_mwk = (30000.0 * INITIAL_FX_RATE) if t == 3 else 0.0

        staff_usd = (base_staff_mwk * ((1.0 + INFLATION_RATE) ** (t - 1))) / fx_rate
        maint_usd = (base_maint_mwk * ((1.0 + INFLATION_RATE) ** (t - 1))) / fx_rate
        battery_usd = (base_battery_mwk * ((1.0 + INFLATION_RATE) ** (t - 1))) / fx_rate
        total_opex = staff_usd + maint_usd + battery_usd

        # EBITDA & EBIT
        ebitda = net_revenue - total_opex
        depr = CAPEX_UPFRONT / USEFUL_LIFE
        ebit = ebitda - depr

        # Debt Service Expenses
        interest_exp = prior_debt_outstanding * INTEREST_RATE
        ebt = ebit - interest_exp

        # Tax calculation with NOL offset
        taxable_income = ebt
        if taxable_income > 0.0 and prior_nol > 0.0:
            if prior_nol >= taxable_income:
                prior_nol -= taxable_income
                taxable_income = 0.0
            else:
                taxable_income -= prior_nol
                prior_nol = 0.0
        elif taxable_income < 0.0:
            prior_nol += -taxable_income
            taxable_income = 0.0

        tax_expense = taxable_income * TAX_RATE if taxable_income > 0.0 else 0.0
        net_income = ebt - tax_expense

        # Working Capital Updates
        ar_ending = (gross_rev_total / 365.0) * 30.0
        ap_ending = (total_opex / 365.0) * 30.0
        change_in_nwc = (ar_ending - prior_ar_ending) - (ap_ending - prior_ap_ending)

        # Cash Flow statements
        cfo = net_income + depr - change_in_nwc
        cfi = 0.0
        cff = -annual_principal_repay
        change_in_cash = cfo + cfi + cff
        cash_ending = prior_cash_ending + change_in_cash

        # Balance Sheet values
        net_ppe = CAPEX_UPFRONT - (prior_acc_depr + depr)
        total_assets = cash_ending + ar_ending + net_ppe
        debt_ending = prior_debt_outstanding - annual_principal_repay
        re_ending = prior_re + net_income

        # Free Cash Flows
        fcff = ebit * (1.0 - TAX_RATE) + depr - change_in_nwc
        fcfe = cfo - annual_principal_repay

        # DSCR
        cfads = ebitda - tax_expense - change_in_nwc
        total_debt_service = interest_exp + annual_principal_repay
        dscr = cfads / total_debt_service if total_debt_service > 0.0 else 99.0

        # Accumulate Row State
        years_list.append({
            'Year': t, 'FX_Rate': fx_rate, 'Generation_kWh': energy_gen_kwh,
            'Revenue_Gross': gross_rev_total, 'Default_Loss': default_loss,
            'MobileMoney_Fee': mm_fee, 'Net_Revenue': net_revenue, 'Staff_Opex': staff_usd,
            'Maint_Opex': maint_usd, 'Battery_Opex': battery_usd, 'Total_Opex': total_opex,
            'EBITDA': ebitda, 'Depreciation': depr, 'EBIT': ebit, 'Interest': interest_exp,
            'EBT': ebt, 'Tax': tax_expense, 'Net_Income': net_income, 'CFO': cfo,
            'CFF': cff, 'Cash_Ending': cash_ending, 'AR': ar_ending, 'Net_PPE': net_ppe,
            'Total_Assets': total_assets, 'AP': ap_ending, 'LongTerm_Debt': debt_ending,
            'PaidIn_Equity': initial_equity, 'Retained_Earnings': re_ending,
            'FCFF': fcff, 'FCFE': fcfe, 'DSCR': dscr
        })

        # Update historical state
        prior_cash_ending = cash_ending
        prior_ar_ending = ar_ending
        prior_ap_ending = ap_ending
        prior_acc_depr += depr
        prior_re = re_ending
        prior_debt_outstanding = debt_ending

    return pd.DataFrame(years_list)

# Solver functions for NPV & IRR
def irr_solver(flows):
    # Numerical secant-method/bisection solver for IRR
    low = -0.99
    high = 3.0
    guess = 0.1
    if not (any(f > 0 for f in flows) and any(f < 0 for f in flows)):
        return np.nan
    for _ in range(150):
        npv_guess = flows[0] + sum(flows[t] / ((1.0 + guess) ** t) for t in range(1, len(flows)))
        if abs(npv_guess) < 1e-7:
            return guess
        npv_low = flows[0] + sum(flows[t] / ((1.0 + low) ** t) for t in range(1, len(flows)))
        if npv_low * npv_guess < 0.0:
            high = guess
        else:
            low = guess
        guess = (low + high) / 2.0
    return guess

# ==============================================================================
# 3. GENERATE THE BEAUTIFIED EXCEL DOCUMENT
# ==============================================================================
def create_beautified_excel(df, filename='Solar_Malawi_Model.xlsx'):
    wb = Workbook()
    
    # ------------------ STYLES DEFINITION ------------------
    font_family = "Segoe UI"
    font_title = Font(name=font_family, size=16, bold=True, color="1E293B")
    font_section = Font(name=font_family, size=12, bold=True, color="0F172A")
    font_header = Font(name=font_family, size=10, bold=True, color="FFFFFF")
    font_bold = Font(name=font_family, size=10, bold=True, color="0F172A")
    font_regular = Font(name=font_family, size=10, color="334155")
    
    fill_header = PatternFill(start_color="1E293B", end_color="1E293B", fill_type="solid")
    fill_zebra = PatternFill(start_color="F8FAFC", end_color="F8FAFC", fill_type="solid")
    fill_summary = PatternFill(start_color="F1F5F9", end_color="F1F5F9", fill_type="solid")
    fill_accent = PatternFill(start_color="FEF3C7", end_color="FEF3C7", fill_type="solid")
    
    border_thin = Side(border_style="thin", color="CBD5E1")
    border_double = Side(border_style="double", color="475569")
    grid_border = Border(left=border_thin, right=border_thin, top=border_thin, bottom=border_thin)
    bottom_double_border = Border(top=border_thin, bottom=border_double)

    align_left = Alignment(horizontal="left", vertical="center")
    align_right = Alignment(horizontal="right", vertical="center")
    align_center = Alignment(horizontal="center", vertical="center")

    # ------------------ TAB 1: EXECUTIVE SUMMARY ------------------
    ws1 = wb.active
    ws1.title = "Executive Summary"
    ws1.views.sheetView[0].showGridLines = True
    
    ws1["A1"] = "Malawi 100kW Solar Mini-Grid Project Finance Model"
    ws1["A1"].font = font_title
    ws1.row_dimensions[1].height = 25
    
    # Run model calculations to get NPV & IRR
    fcff_arr = np.concatenate([[-CAPEX_UPFRONT], df['FCFF'].values])
    fcfe_arr = np.concatenate([[-(CAPEX_UPFRONT * (1 - DEBT_RATIO))], df['FCFE'].values])
    
    project_npv = flows_npv(fcff_arr, DISCOUNT_RATE)
    equity_npv = flows_npv(fcfe_arr, DISCOUNT_RATE)
    project_irr = irr_solver(fcff_arr)
    equity_irr = irr_solver(fcfe_arr)
    
    # Calculate payback period
    cum_fcfe = -(CAPEX_UPFRONT * (1 - DEBT_RATIO))
    payback = 5.0
    for idx, fcfe in enumerate(df['FCFE'].values):
        prior = cum_fcfe
        cum_fcfe += fcfe
        if cum_fcfe >= 0:
            payback = idx + (abs(prior) / fcfe)
            break

    # Calculate LCOE
    pv_costs = CAPEX_UPFRONT
    pv_energy = 0.0
    for idx, row in df.iterrows():
        df_factor = (1 + DISCOUNT_RATE) ** (row['Year'])
        pv_costs += row['Total_Opex'] / df_factor
        pv_energy += row['Generation_kWh'] / df_factor
    lcoe = pv_costs / pv_energy if pv_energy > 0 else 0

    metrics = [
        ("Upfront Capital Required", CAPEX_UPFRONT, "__DOLLAR__{:,.2f}"),
        ("Debt Contribution (70%)", CAPEX_UPFRONT * DEBT_RATIO, "__DOLLAR__{:,.2f}"),
        ("Equity Contribution (30%)", CAPEX_UPFRONT * (1 - DEBT_RATIO), "__DOLLAR__{:,.2f}"),
        ("Project NPV (15% Discount)", project_npv, "__DOLLAR__{:,.2f}"),
        ("Equity NPV (15% Discount)", equity_npv, "__DOLLAR__{:,.2f}"),
        ("Project IRR", project_irr, "{:.2%}"),
        ("Equity IRR", equity_irr, "{:.2%}"),
        ("Equity Payback Period", payback, "{:.2f} Years"),
        ("Levelized Cost of Energy (LCOE)", lcoe, "__DOLLAR__{:,.3f}/kWh"),
        ("Average Debt Service Coverage Ratio (DSCR)", df['DSCR'].mean(), "{:.2f}x"),
        ("Minimum DSCR", df['DSCR'].min(), "{:.2f}x")
    ]
    
    ws1["A3"] = "Key Project Metrics"
    ws1["A3"].font = font_section
    
    ws1.cell(row=4, column=1, value="Metric").font = font_header
    ws1.cell(row=4, column=1).fill = fill_header
    ws1.cell(row=4, column=2, value="Value").font = font_header
    ws1.cell(row=4, column=2).fill = fill_header
    ws1.row_dimensions[4].height = 20

    for r_idx, (m, val, fmt) in enumerate(metrics, start=5):
        cell_m = ws1.cell(row=r_idx, column=1, value=m)
        cell_v = ws1.cell(row=r_idx, column=2, value=val)
        
        cell_m.font = font_bold if "NPV" in m or "IRR" in m else font_regular
        cell_v.font = font_bold if "NPV" in m or "IRR" in m else font_regular
        
        if "NPV" in m or "IRR" in m:
            cell_m.fill = fill_accent
            cell_v.fill = fill_accent
            
        cell_v.number_format = fmt
        cell_m.border = grid_border
        cell_v.border = grid_border
        ws1.row_dimensions[r_idx].height = 18

    # ------------------ TAB 2: FINANCIAL STATEMENTS ------------------
    ws2 = wb.create_sheet(title="Financial Statements")
    ws2.views.sheetView[0].showGridLines = True
    
    ws2["A1"] = "Five-Year Financial Statement Projections"
    ws2["A1"].font = font_title
    
    headers = ["Line Item / Operational Year", "Year 1", "Year 2", "Year 3", "Year 4", "Year 5"]
    for c_idx, h in enumerate(headers, start=1):
        cell = ws2.cell(row=3, column=c_idx, value=h)
        cell.font = font_header
        cell.fill = fill_header
        cell.alignment = align_left if c_idx == 1 else align_right
    ws2.row_dimensions[3].height = 22

    # Assemble line items for display
    line_items = [
        # SECTION: ASSUMPTIONS
        ("Macro Assumptions", "", "TITLE"),
        ("Malawi Kwacha Exchange Rate (MWK/USD)", df['FX_Rate'].values, "FX"),
        ("Annual Solar Generation (kWh)", df['Generation_kWh'].values, "QTY"),
        
        # SECTION: INCOME STATEMENT
        ("Income Statement Projections (USD)", "", "TITLE"),
        ("Gross Billing Revenue", df['Revenue_Gross'].values, "USD"),
        ("Less: Default Loss Provision (8%)", -df['Default_Loss'].values, "USD"),
        ("Less: Mobile Money Transaction Fees (1.5%)", -df['MobileMoney_Fee'].values, "USD"),
        ("Net Operating Revenue", df['Net_Revenue'].values, "USD_BOLD"),
        ("Operating Staffing Opex", -df['Staff_Opex'].values, "USD"),
        ("System Maintenance & Insurance Opex", -df['Maint_Opex'].values, "USD"),
        ("Battery Replacement Opex (Year 3)", -df['Battery_Opex'].values, "USD"),
        ("Total Operating Expenses", -df['Total_Opex'].values, "USD_SUBTOTAL"),
        ("EBITDA (Earnings Before Interest, Taxes, Depr)", df['EBITDA'].values, "USD_BOLD"),
        ("Less: Plant Depreciation (10-Yr Straight Line)", -df['Depreciation'].values, "USD"),
        ("EBIT (Operating Profit)", df['EBIT'].values, "USD_BOLD"),
        ("Less: Local Debt Interest Expense (22%)", -df['Interest'].values, "USD"),
        ("Earnings Before Taxes (EBT)", df['EBT'].values, "USD_BOLD"),
        ("Less: Corporate Taxes Paid (30% net of NOLs)", -df['Tax'].values, "USD"),
        ("Net Cash Income (After-Tax Profit)", df['Net_Income'].values, "USD_TOTAL"),

        # SECTION: CASH FLOWS
        ("Cash Flow Statement Projections (USD)", "", "TITLE"),
        ("Net After-Tax Profit (Net Income)", df['Net_Income'].values, "USD"),
        ("Add: Non-Cash Depreciation Expense", df['Depreciation'].values, "USD"),
        ("Less: Net Working Capital Changes", -(df['AR'].diff().fillna(df['AR'].iloc[0]) - df['AP'].diff().fillna(df['AP'].iloc[0])).values, "USD"),
        ("Net Cash Flow from Operations (CFO)", df['CFO'].values, "USD_BOLD"),
        ("Net Cash Flow from Investing (CFI)", np.zeros(5), "USD"),
        ("Net Cash Flow from Financing (CFF - Repayment)", df['CFF'].values, "USD"),
        ("Net Change in Annual Liquid Cash", (df['CFO'] + df['CFF']).values, "USD_SUBTOTAL"),
        ("Liquid Cash Ending Balance", df['Cash_Ending'].values, "USD_TOTAL"),

        # SECTION: BALANCE SHEET
        ("Balance Sheet Projections (USD)", "", "TITLE"),
        ("Current Asset: Cash Balance", df['Cash_Ending'].values, "USD"),
        ("Current Asset: Accounts Receivable (30 Days)", df['AR'].values, "USD"),
        ("Non-Current Asset: Net PP&E", df['Net_PPE'].values, "USD"),
        ("Total Assets Owned", df['Total_Assets'].values, "USD_TOTAL"),
        ("Current Liability: Accounts Payable (30 Days)", df['AP'].values, "USD"),
        ("Non-Current Liability: Outstanding Senior Debt", df['LongTerm_Debt'].values, "USD"),
        ("Equity Component: Upfront Paid-In Capital", [df['PaidIn_Equity'].iloc[0]] * 5, "USD"),
        ("Equity Component: Accumulated Retained Earnings", df['Retained_Earnings'].values, "USD"),
        ("Total Liabilities and Equity Financed", df['Total_Assets'].values, "USD_TOTAL"),

        # SECTION: CASH ANALYSIS
        ("Debt & Leverage Performance Metrics", "", "TITLE"),
        ("Debt Service Coverage Ratio (DSCR)", df['DSCR'].values, "RATIO"),
        ("Free Cash Flow to Firm (FCFF)", df['FCFF'].values, "USD_BOLD"),
        ("Free Cash Flow to Equity (FCFE)", df['FCFE'].values, "USD_BOLD")
    ]

    curr_row = 4
    for label, vals, style in line_items:
        if style == "TITLE":
            curr_row += 1
            cell = ws2.cell(row=curr_row, column=1, value=label)
            cell.font = font_section
            ws2.row_dimensions[curr_row].height = 20
            continue
            
        cell_l = ws2.cell(row=curr_row, column=1, value=label)
        cell_l.border = grid_border
        
        # Apply visual hierarchy styles
        if "BOLD" in style:
            cell_l.font = font_bold
            cell_l.fill = fill_summary
        elif "TOTAL" in style:
            cell_l.font = font_bold
            cell_l.fill = fill_accent
            cell_l.border = bottom_double_border
        elif "SUBTOTAL" in style:
            cell_l.font = font_bold
            cell_l.border = Border(top=border_thin, bottom=border_thin)
        else:
            cell_l.font = font_regular
            if curr_row % 2 == 0:
                cell_l.fill = fill_zebra

        for yr_idx in range(5):
            val = vals[yr_idx]
            cell_v = ws2.cell(row=curr_row, column=yr_idx+2, value=val)
            cell_v.alignment = align_right
            cell_v.border = cell_l.border
            cell_v.fill = cell_l.fill
            cell_v.font = cell_l.font

            # Apply specific number formatting
            if "USD" in style:
                cell_v.number_format = "__DOLLAR__{:,.2f}" if val >= 0 else "(__DOLLAR__{:,.2f})"
            elif style == "FX":
                cell_v.number_format = "#,##0.00"
            elif style == "QTY":
                cell_v.number_format = "#,##0"
            elif style == "RATIO":
                cell_v.number_format = "0.00x"

        ws2.row_dimensions[curr_row].height = 18
        curr_row += 1

    # Auto-fit column widths
    for col in ws1.columns:
        max_len = max(len(str(cell.value or '')) for cell in col)
        col_letter = col[0].column_letter
        ws1.column_dimensions[col_letter].width = max(max_len + 3, 12)

    for col in ws2.columns:
        max_len = max(len(str(cell.value or '')) for cell in col)
        col_letter = col[0].column_letter
        ws2.column_dimensions[col_letter].width = max(max_len + 3, 12)

    wb.save(filename)
    print(f"Workbook successfully saved as: '{filename}'")

def flows_npv(flows, r):
    return flows[0] + sum(flows[t] / ((1.0 + r) ** t) for t in range(1, len(flows)))

# ==============================================================================
# 4. RUN SYSTEM TEST
# ==============================================================================
if __name__ == '__main__':
    print("=== WorldQuant Solar Mini-Grid Financial Model ===")
    df_results = run_deterministic_model()
    create_beautified_excel(df_results)
""".replace("__DOLLAR__", "$")
    }
}

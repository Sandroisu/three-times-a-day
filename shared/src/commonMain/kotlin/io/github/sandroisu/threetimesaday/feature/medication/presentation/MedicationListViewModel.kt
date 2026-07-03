package io.github.sandroisu.threetimesaday.feature.medication.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MedicationListViewModel(
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(MedicationListUiState())
    val uiState: StateFlow<MedicationListUiState> = mutableUiState.asStateFlow()

    init {
        loadMedications()
    }

    fun loadMedications() {
        viewModelScope.launch {
            mutableUiState.update { currentState ->
                currentState.copy(isLoading = true, errorMessage = null)
            }
            try {
                val medications = medicationRepository.getMedications()
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        medications = medications.map { medication -> toListItem(medication) },
                        errorMessage = null
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = loadFailure.message ?: "Не удалось загрузить препараты"
                    )
                }
            }
        }
    }

    private fun toListItem(medication: Medication): MedicationListItemUiModel = MedicationListItemUiModel(
        id = medication.id,
        name = medication.name,
        dosageText = medication.dosageText,
        intakeRuleText = medicationIntakeRuleText(medication.intakeRule)
    )
}

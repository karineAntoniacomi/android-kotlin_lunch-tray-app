/*
 * Copyright (C) 2021 The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.lunchtray.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.example.lunchtray.data.DataSource
import java.text.NumberFormat

class OrderViewModel : ViewModel() {

    // Map of menu items
    val menuItems = DataSource.menuItems

    // Default values for item prices - usadas para rastrear o item selecionado
    // anteriormente caso seja trocado antes de passar para a próxima tela (subtotal
    // é atualizado à medida que o usuário seleciona uma opção)
    private var previousEntreePrice = 0.0
    private var previousSidePrice = 0.0
    private var previousAccompanimentPrice = 0.0

    // Default tax rate
    private val taxRate = 0.08

    // Entree for the order - variaveis privadas para armazenar a opção selecionada
    // no momento, acompanhadas de propriedade de apoio pública
    private val _entree = MutableLiveData<MenuItem?>()

    // A classe MenuItem do objeto LiveData é anulável, pois o usuário
    // tem a opção de não selecionar todas as opções
    val entree: LiveData<MenuItem?> = _entree

    // Side for the order
    private val _side = MutableLiveData<MenuItem?>()
    val side: LiveData<MenuItem?> = _side

    // Accompaniment for the order
    private val _accompaniment = MutableLiveData<MenuItem?>()
    val accompaniment: LiveData<MenuItem?> = _accompaniment

    // Subtotal for the order
    private val _subtotal = MutableLiveData(0.0)
    val subtotal: LiveData<String> = Transformations.map(_subtotal) {
        NumberFormat.getCurrencyInstance().format(it)
    }

    // Total cost of the order
    private val _total = MutableLiveData(0.0)
    val total: LiveData<String> = Transformations.map(_total) {
        NumberFormat.getCurrencyInstance().format(it)
    }

    // Tax for the order
    private val _tax = MutableLiveData(0.0)
    val tax: LiveData<String> = Transformations.map(_tax) {
        NumberFormat.getCurrencyInstance().format(it)
    }

    /** * Set the entree for the order */
    fun setEntree(entree: String) {
        /* Caso a _entree não seja null, (usuário já selecionou uma entrada, mas mudou
        de ideia) define previousEntreePrice como o preço da current _entree */
        // Elvis operator ?: Se o primeiro operando não é nulo, será devolvido, se for nulo,
        // então o segundo operando é devolvido.
        previousEntreePrice = _entree.value?.price ?: 0.0

        /* Se _subtotal.value não for nulo, subtrai o preço de entrada anterior previousEntreePrice do
         atual valor subtotal. Isso garante que se cobra apenas pela entrada atualmente selecionada */
        _subtotal.value = (_subtotal.value ?: 0.0) - previousEntreePrice

        /* Define o valor de entrada atual _entree para o item de menu correspondente à
        string passada para a função. Acessa o MenuItem usando menuItems */
        _entree.value = menuItems[entree]

        /* Atualiza o subtotal chamando updateSubtotal() e transmite o preço do último prato
        principal selecionado, para refletir o preço da entrada selecionada */
        updateSubtotal(menuItems[entree]?.price ?: 0.0)
    }

    /** * Set the side for the order */
    fun setSide(side: String) {
        /* Caso a _side.value não seja null, (usuário já selecionou um side, mas mudou
        de ideia) define previousEntreePrice como o preço da current side  */
        previousSidePrice = _side.value?.price ?: 0.0

        /* Se _subtotal.value não for nulo, subtrai o preço de side anterior previousSidePrice do
        atual valor subtotal. Isso garante que se cobra apenas pela side atualmente selecionada */
        _subtotal.value = (_subtotal.value ?: 0.0) - previousSidePrice

        /* Define o valor de side atual _entree para o item de menu correspondente à string
        passada para a função. Acessa o MenuItem usando menuItems */
        _side.value = menuItems[side]

        /* Atualiza o subtotal chamando updateSubtotal() e transmite o preço do último prato
        principal selecionado, para refletir o preço da entrada selecionada */
        updateSubtotal(menuItems[side]?.price ?: 0.0)
    }

    /** * Set the accompaniment for the order. */
    fun setAccompaniment(accompaniment: String) {
        /* Se _accompaniment.value não for nulo, defina o preço do acompanhamento anterior
        para o preço atual do acompanhamento */
        previousAccompanimentPrice = _accompaniment.value?.price ?: 0.0

        /* Se _accompaniment.value não for nulo, subtraia o preço do acompanhamento anterior
         de o valor subtotal atual. Isso garante que cobramos apenas pelos itens atualmente
        selecionados acompanhamento */
        _subtotal.value = (_subtotal.value ?: 0.0) - previousAccompanimentPrice

        /* Define o valor do acompanhamento atual para o item de menu correspondente ao
        passado corda */
        _accompaniment.value = menuItems[accompaniment]

        // Atualiza o subtotal para refletir o preço do acompanhamento selecionado
        updateSubtotal(menuItems[accompaniment]?.price ?: 0.0)
    }

    /** * Update subtotal value. */
    private fun updateSubtotal(itemPrice: Double) {
        /* Se _subtotal.value não for nulo, atualiza para refletir o preço do último
        item adicionado (adiciona o itemPrice ao _subtotal). Caso contrário, defina
        _subtotal.value para igualar o preço do item */
        if (_subtotal.value != null) {
            // plus contém os elementos da coleção original e (+) do segundo operando
            _subtotal.value = (_subtotal.value)?.plus(itemPrice)
        } else {
            _subtotal.value = itemPrice
        }

        /* Calcula o imposto e o total resultante (Chamar calculateTaxAndTotal() depois que
        o _subtotal tiver sido definido/atualizado, para que os valores sejam atualizados
        refletindo no subtotal) */
        calculateTaxAndTotal()
    }

    /** * Calculate tax and update total. */
    fun calculateTaxAndTotal() {
        /* Define _tax.value com base no subtotal e na taxa de imposto (alíquota
        multiplicada pelo subtotal). Funcao times multiplica o valor por outro valor */
        _tax.value = _subtotal.value?.times(taxRate)

        // Define o total com base no subtotal e _tax.value (subtotal mais os tributos)
        _total.value = _subtotal.value?.plus(_tax.value ?: 0.0)
    }

    /** * Reset all values pertaining to the order. Call when the user submits or cancels an order. */
    fun resetOrder() {
        // Redefine para o valor original (0,0 ou nulo) todos os valores associados a um pedido
        previousEntreePrice = 0.0
        previousSidePrice = 0.0
        previousAccompanimentPrice = 0.0
        _entree.value = null
        _side.value = null
        _accompaniment.value = null
        _subtotal.value = 0.0
        _total.value = 0.0
        _tax.value = 0.0
    }
}

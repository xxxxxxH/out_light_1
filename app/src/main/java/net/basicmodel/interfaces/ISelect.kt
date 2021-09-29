package net.basicmodel.interfaces

import net.basicmodel.view.MyLightView

interface ISelect {
    fun onChanged(myLightView: MyLightView?, position: Int)
    fun onSelected(myLightView: MyLightView?, position: Int)
}
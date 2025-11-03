/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.analysis.ui;


import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@ActionID(
    category = "Tools",
    id = "com.injurytime.dossier.ui.ShowDossierAction"
)
@ActionRegistration(
    displayName = "#CTL_ShowDossier"
)
@ActionReference(path = "Menu/Tools", position = 1865) // tweak position as you like
@Messages({
    "CTL_ShowDossier=Dossier…"
})
public final class ShowDossierAction implements ActionListener {
  @Override public void actionPerformed(ActionEvent e) {
    FixtureDossierTopComponent tc = new FixtureDossierTopComponent();
    tc.open();
    tc.requestActive();
  }
}


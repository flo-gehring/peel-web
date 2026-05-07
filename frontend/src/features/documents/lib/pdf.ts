import jsPDF from 'jspdf'

type RenderDocumentPdfParams = {
  body: string
}

export async function renderDocumentPdf({ body }: RenderDocumentPdfParams): Promise<string> {
  const doc = new jsPDF({
    orientation: 'portrait',
    unit: 'pt',
    format: 'a4',
  })

  const pageWidth = doc.internal.pageSize.getWidth()
  const pageHeight = doc.internal.pageSize.getHeight()
  const margin = 56
  const maxTextWidth = pageWidth - margin * 2
  const lineHeight = 16

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(12)

  const renderedBody = body.trim().length > 0 ? body : ' '
  const sourceLines = renderedBody.split('\n')

  let cursorY = margin
  for (const sourceLine of sourceLines) {
    const wrappedLines = doc.splitTextToSize(sourceLine.length > 0 ? sourceLine : ' ', maxTextWidth) as string[]
    for (const wrappedLine of wrappedLines) {
      if (cursorY > pageHeight - margin) {
        doc.addPage()
        cursorY = margin
      }
      doc.text(wrappedLine, margin, cursorY)
      cursorY += lineHeight
    }
  }

  const blob = doc.output('blob')
  return URL.createObjectURL(blob)
}
